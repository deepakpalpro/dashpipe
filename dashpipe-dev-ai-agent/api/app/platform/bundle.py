"""Convert proposed pipeline designs to dashpipe import bundles."""

from __future__ import annotations

from datetime import UTC, datetime
from typing import Any

from app.schemas import ProposedPipeline, ProposedStep


def step_to_bundle(step: ProposedStep) -> dict[str, Any]:
    return {
        "pipelet_id": step.pipelet_id,
        "step_order": step.step_order,
        "deployment_config": step.deployment_config or {},
        "execution_config": step.execution_config or {},
        "connector_refs": list(step.connector_ids),
        "service_refs": list(step.service_ids),
        "input_queue": None,
        "output_queue": None,
    }


def proposed_to_bundle(proposed: ProposedPipeline) -> dict[str, Any]:
    """Build an import bundle matching dashpipe-demo pipeline JSON shape."""
    return {
        "format_version": "1",
        "exported_at": datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "pipeline": {
            "name": proposed.name,
            "description": proposed.description or "",
            "visibility": "private",
            "execution_mode": "async",
            "deployment_config": {},
            "execution_config": {},
        },
        "connectors": [],
        "services": [],
        "steps": [step_to_bundle(step) for step in proposed.steps],
    }
