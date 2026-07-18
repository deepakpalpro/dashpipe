"""Parse pipeline guide LLM JSON into API response models."""

from __future__ import annotations

import json
import re
from typing import Any

from app.agents.knowledge_base import KnowledgeBase
from app.schemas import PipelineGuideResponse, ProposedPipeline, ProposedStep


def kb_fallback(kb: KnowledgeBase, message: str) -> PipelineGuideResponse | None:
    lower = message.lower()
    best, best_score = None, 0
    for pattern in kb.patterns:
        match_tokens = pattern.get("match") or []
        if not isinstance(match_tokens, list):
            continue
        score = sum(1 for t in match_tokens if isinstance(t, str) and t.lower() in lower)
        if score > best_score:
            best_score, best = score, pattern
    if not best or best_score == 0:
        return None
    steps_raw = best.get("steps") or []
    steps: list[ProposedStep] = []
    for order, step_id in enumerate(steps_raw, start=1):
        if not isinstance(step_id, str):
            continue
        entry = kb.catalog_entry(step_id) or {}
        steps.append(
            ProposedStep(
                pipelet_id=step_id,
                step_order=order,
                execution_config=dict(entry.get("execution_config") or {}),
                deployment_config=dict(entry.get("deployment_config") or {}),
            )
        )
    if not steps:
        return None
    name = best.get("name") or "Suggested pipeline"
    notes = best.get("notes") or ""
    thinking = (
        f"LLM unavailable. Matched KB pattern '{best.get('id', '?')}' (score {best_score}). "
        f"Export JSON and import into Dataflow builder. {notes}"
    )
    return PipelineGuideResponse(
        reply=f"Used offline pattern: {name}. Review steps and export JSON.",
        thinking=thinking,
        proposed_pipeline=ProposedPipeline(
            name=name,
            description=best.get("description") or "",
            steps=steps,
        ),
    )


def parse_pipeline_output(
    raw: str,
    kb: KnowledgeBase,
    allowed_ids: set[str],
) -> PipelineGuideResponse:
    root = extract_json(raw)
    if root is None:
        return PipelineGuideResponse(
            reply=(raw or "").strip(),
            thinking=preamble(raw),
            raw_model_output=raw,
        )
    thinking = root.get("thinking") or preamble(raw)
    reply = root.get("reply") or (raw or "").strip()
    proposed_node = root.get("proposed_pipeline")
    if not isinstance(proposed_node, dict):
        return PipelineGuideResponse(reply=reply, thinking=thinking, raw_model_output=raw)
    proposed = parse_proposed(kb, proposed_node, allowed_ids)
    return PipelineGuideResponse(
        reply=reply,
        thinking=thinking,
        proposed_pipeline=proposed,
        raw_model_output=raw,
    )


def parse_proposed(
    kb: KnowledgeBase,
    node: dict[str, Any],
    allowed_ids: set[str],
) -> ProposedPipeline | None:
    steps_node = node.get("steps")
    if not isinstance(steps_node, list) or not steps_node:
        return None
    steps: list[ProposedStep] = []
    fallback_order = 1
    for step_node in steps_node:
        if not isinstance(step_node, dict):
            continue
        pipelet_id = step_node.get("pipelet_id") or step_node.get("pipeletId")
        if not pipelet_id or (allowed_ids and pipelet_id not in allowed_ids):
            continue
        order = step_node.get("step_order") or step_node.get("stepOrder") or fallback_order
        fallback_order = int(order) + 1
        steps.append(
            ProposedStep(
                pipelet_id=pipelet_id,
                step_order=int(order),
                execution_config=merge_config(kb, pipelet_id, "execution_config", step_node),
                deployment_config=merge_config(kb, pipelet_id, "deployment_config", step_node),
                connector_ids=str_list(
                    step_node.get("connector_ids") or step_node.get("connectorIds")
                ),
                service_ids=str_list(
                    step_node.get("service_ids") or step_node.get("serviceIds")
                ),
            )
        )
    if not steps:
        return None
    steps.sort(key=lambda s: s.step_order)
    for i, step in enumerate(steps, start=1):
        step.step_order = i
    return ProposedPipeline(
        name=node.get("name") or "Suggested pipeline",
        description=node.get("description") or "",
        steps=steps,
    )


def merge_config(
    kb: KnowledgeBase,
    pipelet_id: str,
    field: str,
    step_node: dict[str, Any],
) -> dict[str, Any]:
    alt = "executionConfig" if field == "execution_config" else "deploymentConfig"
    model_val = step_node.get(field) or step_node.get(alt) or {}
    if not isinstance(model_val, dict):
        model_val = {}
    entry = kb.catalog_entry(pipelet_id) or {}
    kb_val = entry.get(field) or {}
    if not isinstance(kb_val, dict):
        kb_val = {}
    merged = dict(kb_val)
    merged.update({k: v for k, v in model_val.items() if v is not None})
    return merged


def str_list(value: Any) -> list[str]:
    if not isinstance(value, list):
        return []
    return [str(v) for v in value if v]


def preamble(raw: str | None) -> str | None:
    if not raw:
        return None
    start = raw.find("{")
    if start <= 0:
        return None
    pre = raw[:start].strip()
    if not pre or pre.startswith("```"):
        return None
    return pre[:1200] + ("…" if len(pre) > 1200 else "")


def extract_json(raw: str | None) -> dict[str, Any] | None:
    if not raw or not raw.strip():
        return None
    text = raw.strip()
    for candidate in (text, fenced_json(text), brace_slice(text)):
        if candidate is None:
            continue
        try:
            data = json.loads(candidate)
            if isinstance(data, dict):
                return data
        except json.JSONDecodeError:
            continue
    return None


def fenced_json(text: str) -> str | None:
    match = re.search(r"```(?:json)?\s*\n(.*?)```", text, re.DOTALL | re.IGNORECASE)
    return match.group(1).strip() if match else None


def brace_slice(text: str) -> str | None:
    start, end = text.find("{"), text.rfind("}")
    if start >= 0 and end > start:
        return text[start : end + 1]
    return None
