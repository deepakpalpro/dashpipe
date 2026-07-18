"""LangGraph ReAct loop for platform operations."""

from __future__ import annotations

import json
from typing import Any, TypedDict

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_core.tools import BaseTool
from langgraph.prebuilt import create_react_agent

from app.llm.providers import LlmProvider, default_model, get_chat_model
from app.schemas import OpsAgentRequest, OpsAgentResponse, OpsToolCallLog

OPS_SYSTEM = """
You are the Dashpipe Platform Ops assistant for the configured tenant.
Help users manage pipelines, executions, connectors, and the pipelet catalog.

Guidelines:
- Prefer dry_run_pipeline before run_pipeline when validating changes.
- On execution failures, call debug_execution for pipeline_id and execution_id.
- Use list_pipelets(active_only=True) for live catalog over static assumptions.
- Be concise; cite pipeline and execution ids when relevant.
- Never invent ids — use tool results.
""".strip()


class OpsGraphState(TypedDict, total=False):
    request: OpsAgentRequest
    messages: list[BaseMessage]
    response: OpsAgentResponse


def _build_user_message(request: OpsAgentRequest) -> str:
    parts = [request.message.strip()]
    if request.pipeline_id:
        parts.append(f"Context pipeline_id: {request.pipeline_id}")
    if request.execution_id:
        parts.append(f"Context execution_id: {request.execution_id}")
    if request.proposed_pipeline:
        parts.append(
            "Context proposed_pipeline JSON:\n"
            + json.dumps(request.proposed_pipeline, indent=2, default=str)
        )
    return "\n\n".join(parts)


def _extract_tool_logs(messages: list[BaseMessage]) -> list[OpsToolCallLog]:
    logs: list[OpsToolCallLog] = []
    pending: dict[str, str] = {}
    for msg in messages:
        if isinstance(msg, AIMessage) and msg.tool_calls:
            for call in msg.tool_calls:
                pending[call["id"]] = call["name"]
                logs.append(
                    OpsToolCallLog(
                        tool=call["name"],
                        args=call.get("args") or {},
                        result=None,
                    )
                )
        if isinstance(msg, ToolMessage):
            name = pending.get(msg.tool_call_id, msg.name or "tool")
            for entry in reversed(logs):
                if entry.tool == name and entry.result is None:
                    entry.result = _truncate(msg.content)
                    break
    return logs


def _truncate(content: Any, limit: int = 4000) -> str:
    text = content if isinstance(content, str) else json.dumps(content, default=str)
    if len(text) <= limit:
        return text
    return text[: limit - 3] + "..."


def _extract_reply(messages: list[BaseMessage]) -> str:
    for msg in reversed(messages):
        if isinstance(msg, AIMessage) and msg.content:
            content = msg.content
            if isinstance(content, list):
                content = "".join(
                    block.get("text", "") if isinstance(block, dict) else str(block)
                    for block in content
                )
            if isinstance(content, str) and content.strip():
                return content.strip()
    return "No response from ops agent."


def _extract_refs(messages: list[BaseMessage]) -> tuple[str | None, str | None]:
    pipeline_id: str | None = None
    execution_id: str | None = None
    for msg in messages:
        if not isinstance(msg, ToolMessage):
            continue
        try:
            data = json.loads(msg.content) if isinstance(msg.content, str) else msg.content
        except (json.JSONDecodeError, TypeError):
            continue
        if isinstance(data, dict):
            pipeline_id = pipeline_id or _pick_id(data, ("id", "pipeline_id", "pipelineId"))
            execution_id = execution_id or _pick_id(data, ("execution_id", "executionId", "id"))
            nested = data.get("execution")
            if isinstance(nested, dict):
                execution_id = execution_id or _pick_id(nested, ("id", "execution_id", "executionId"))
    return pipeline_id, execution_id


def _pick_id(data: dict[str, Any], keys: tuple[str, ...]) -> str | None:
    for key in keys:
        value = data.get(key)
        if value:
            return str(value)
    return None


def run_ops_agent(
    request: OpsAgentRequest,
    tools: list[BaseTool],
    *,
    model: BaseChatModel | None = None,
) -> OpsAgentResponse:
    provider = LlmProvider.from_wire(request.provider)
    chat_model = model or get_chat_model(
        provider,
        (request.model or default_model(provider)).strip(),
        api_key=request.api_key,
        code_mode=False,
    )
    agent = create_react_agent(chat_model, tools, prompt=SystemMessage(content=OPS_SYSTEM))
    user_text = _build_user_message(request)
    result = agent.invoke({"messages": [HumanMessage(content=user_text)]})
    messages: list[BaseMessage] = result.get("messages") or []
    tool_logs = _extract_tool_logs(messages)
    pipeline_id, execution_id = _extract_refs(messages)
    pipeline_id = request.pipeline_id or pipeline_id
    execution_id = request.execution_id or execution_id
    return OpsAgentResponse(
        reply=_extract_reply(messages),
        tool_calls=tool_logs,
        pipeline_id=pipeline_id,
        execution_id=execution_id,
        model=f"{provider.value}/{(request.model or default_model(provider)).strip()}",
    )
