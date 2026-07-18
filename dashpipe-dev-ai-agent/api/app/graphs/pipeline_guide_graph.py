"""LangGraph workflow: LLM pipeline guide with KB fallback."""

from __future__ import annotations

import json
from typing import Any, TypedDict

from langgraph.graph import END, StateGraph

from app.agents.knowledge_base import KnowledgeBase
from app.agents.pipeline_parser import kb_fallback, parse_pipeline_output
from app.config import settings
from app.llm.exceptions import LlmUnavailableError
from app.llm.providers import LlmProvider, default_model, invoke_chat
from app.schemas import PipelineGuideRequest, PipelineGuideResponse

SYSTEM_HEADER = """
You are the AI Dataflow Developer — a pipeline design assistant for the Dataflow platform.
Help users design linear pipelines: Source → Processor(s) → Destination using pipelet ids.

When proposing a pipeline, respond with a single JSON object only (no markdown fences):
{
  "thinking": "brief reasoning (why these pipelets)",
  "reply": "short human explanation",
  "proposed_pipeline": {
    "name": "Suggested name",
    "description": "one sentence",
    "steps": [
      {
        "pipelet_id": "plet-...",
        "step_order": 1,
        "execution_config": {},
        "deployment_config": {},
        "connector_ids": [],
        "service_ids": []
      }
    ]
  }
}
Always include "thinking". If only answering a question, set proposed_pipeline null.
Never include secrets. Leave connector_ids and service_ids as [].
""".strip()


class GuideGraphState(TypedDict, total=False):
    request: PipelineGuideRequest
    provider: str
    model: str
    kb_context: str
    user_payload: str
    allowed_ids: list[str]
    raw: str
    llm_error: str
    response: PipelineGuideResponse
    model_label: str


def _build_user_payload(request: PipelineGuideRequest) -> str:
    payload: dict[str, Any] = {"user_message": request.message.strip()}
    payload["request_catalog_ids"] = [c.id for c in request.catalog[:40] if c.id]
    payload["current_steps"] = [
        {"pipelet_id": s.get("pipelet_id"), "step_order": s.get("step_order")}
        for s in request.current_steps
        if s.get("pipelet_id")
    ]
    return json.dumps(payload)


def _allowed_ids(kb: KnowledgeBase, request: PipelineGuideRequest) -> set[str]:
    ids = set(kb.known_pipelet_ids())
    ids.update(c.id for c in request.catalog if c.id)
    return ids


def _call_llm_node(state: GuideGraphState) -> GuideGraphState:
    provider = LlmProvider.from_wire(state["provider"])
    model = state["model"]
    system = f"{SYSTEM_HEADER}\n\n{state['kb_context']}"
    try:
        raw = invoke_chat(
            provider,
            model=model,
            system=system,
            user=state["user_payload"],
            api_key=state["request"].api_key,
            code_mode=False,
        )
        return {
            "raw": raw,
            "llm_error": "",
            "model_label": f"{provider.value}/{model}",
        }
    except LlmUnavailableError as exc:
        return {"llm_error": str(exc), "raw": "", "model_label": f"{provider.value}/{model}"}


def _parse_node(state: GuideGraphState, kb: KnowledgeBase) -> GuideGraphState:
    allowed = set(state["allowed_ids"])
    response = parse_pipeline_output(state.get("raw", ""), kb, allowed)
    return {
        "response": response.model_copy(
            update={
                "model": state.get("model_label"),
                "raw_model_output": state.get("raw"),
            }
        )
    }


def _fallback_node(state: GuideGraphState, kb: KnowledgeBase) -> GuideGraphState:
    fallback = kb_fallback(kb, state["request"].message)
    if fallback is None:
        raise LlmUnavailableError(state.get("llm_error") or "LLM unavailable and no KB match")
    error_note = state.get("llm_error") or "LLM unavailable"
    return {
        "response": fallback.model_copy(
            update={
                "model": "kb-fallback",
                "raw_model_output": f"LLM error: {error_note}\n\nUsed KB pattern match.",
            }
        )
    }


def _route_after_llm(state: GuideGraphState) -> str:
    if state.get("llm_error"):
        return "fallback"
    return "parse"


def build_pipeline_guide_graph(kb: KnowledgeBase | None = None):
    knowledge = kb or KnowledgeBase()

    def parse_wrapper(state: GuideGraphState) -> GuideGraphState:
        return _parse_node(state, knowledge)

    def fallback_wrapper(state: GuideGraphState) -> GuideGraphState:
        return _fallback_node(state, knowledge)

    graph = StateGraph(GuideGraphState)
    graph.add_node("call_llm", _call_llm_node)
    graph.add_node("parse", parse_wrapper)
    graph.add_node("fallback", fallback_wrapper)
    graph.set_entry_point("call_llm")
    graph.add_conditional_edges(
        "call_llm",
        _route_after_llm,
        {"parse": "parse", "fallback": "fallback"},
    )
    graph.add_edge("parse", END)
    graph.add_edge("fallback", END)
    return graph.compile()


def run_pipeline_guide(
    request: PipelineGuideRequest,
    kb: KnowledgeBase | None = None,
) -> PipelineGuideResponse:
    if not settings.enabled:
        raise LlmUnavailableError("Agent is disabled")

    knowledge = kb or KnowledgeBase()
    provider = LlmProvider.from_wire(request.provider)
    model = (request.model or default_model(provider)).strip()

    initial: GuideGraphState = {
        "request": request,
        "provider": provider.value,
        "model": model,
        "kb_context": knowledge.compact_prompt_context(),
        "user_payload": _build_user_payload(request),
        "allowed_ids": sorted(_allowed_ids(knowledge, request)),
    }

    compiled = build_pipeline_guide_graph(knowledge)
    final = compiled.invoke(initial)
    response = final.get("response")
    if response is None:
        raise LlmUnavailableError("Pipeline guide produced no response")
    return response
