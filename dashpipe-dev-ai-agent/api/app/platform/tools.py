"""LangChain tool bindings over shared platform_ops."""

from __future__ import annotations

import json
from typing import Any

from langchain_core.tools import BaseTool, StructuredTool

from app.platform.settings import PlatformSettings
from dashpipe_mcp.client import DashpipeApiError
from dashpipe_mcp.platform_ops import PlatformOps


def _serialize(result: Any) -> str:
    return json.dumps(result, indent=2, default=str)


def _serialize_error(exc: Exception) -> str:
    if isinstance(exc, DashpipeApiError):
        return json.dumps({"error": exc.detail, "status_code": exc.status_code}, indent=2)
    return json.dumps({"error": str(exc)}, indent=2)


def _safe(fn: Any, *args: Any, **kwargs: Any) -> str:
    try:
        return _serialize(fn(*args, **kwargs))
    except Exception as exc:  # noqa: BLE001
        return _serialize_error(exc)


def build_platform_tools(settings: PlatformSettings | None = None) -> list[BaseTool]:
    cfg = settings or PlatformSettings.from_env()
    ops = PlatformOps(cfg.to_mcp_settings())

    return [
        StructuredTool.from_function(
            name="dashpipe_health",
            description="Check dashpipe-api actuator health (no tenant header required).",
            func=lambda: _safe(ops.dashpipe_health),
        ),
        StructuredTool.from_function(
            name="dashpipe_config",
            description="Show effective API URL, tenant, and pipelet catalog path.",
            func=lambda: _safe(ops.dashpipe_config),
        ),
        StructuredTool.from_function(
            name="list_pipelines",
            description="List all pipelines for the configured tenant.",
            func=lambda: _safe(ops.list_pipelines),
        ),
        StructuredTool.from_function(
            name="get_pipeline",
            description="Get a pipeline by id including its steps.",
            func=lambda pipeline_id: _safe(ops.get_pipeline, pipeline_id),
        ),
        StructuredTool.from_function(
            name="create_pipeline",
            description="Create a new pipeline in draft status.",
            func=lambda name, description="", visibility="private", execution_mode="async", deployment_config=None, execution_config=None: _safe(
                ops.create_pipeline, name, description, visibility, execution_mode, deployment_config, execution_config
            ),
        ),
        StructuredTool.from_function(
            name="update_pipeline",
            description="Update pipeline metadata and status.",
            func=lambda pipeline_id, name, description="", visibility="private", execution_mode="async", status="draft", deployment_config=None, execution_config=None: _safe(
                ops.update_pipeline, pipeline_id, name, description, visibility, execution_mode, status, deployment_config, execution_config
            ),
        ),
        StructuredTool.from_function(
            name="activate_pipeline",
            description="Activate a pipeline (required before run).",
            func=lambda pipeline_id: _safe(ops.activate_pipeline, pipeline_id),
        ),
        StructuredTool.from_function(
            name="deactivate_pipeline",
            description="Deactivate a pipeline (sets status back to draft).",
            func=lambda pipeline_id: _safe(ops.deactivate_pipeline, pipeline_id),
        ),
        StructuredTool.from_function(
            name="archive_pipeline",
            description="Archive (soft-delete) a pipeline.",
            func=lambda pipeline_id: _safe(ops.archive_pipeline, pipeline_id),
        ),
        StructuredTool.from_function(
            name="replace_pipeline_steps",
            description="Replace the full step graph for a pipeline.",
            func=lambda pipeline_id, steps: _safe(ops.replace_pipeline_steps, pipeline_id, steps),
        ),
        StructuredTool.from_function(
            name="dry_run_pipeline",
            description="Validate a pipeline without executing it.",
            func=lambda pipeline_id: _safe(ops.dry_run_pipeline, pipeline_id),
        ),
        StructuredTool.from_function(
            name="run_pipeline",
            description="Start a pipeline execution. Pipeline must be active.",
            func=lambda pipeline_id, payload=None: _safe(ops.run_pipeline, pipeline_id, payload),
        ),
        StructuredTool.from_function(
            name="export_pipeline",
            description="Export a pipeline bundle for portability.",
            func=lambda pipeline_id: _safe(ops.export_pipeline, pipeline_id),
        ),
        StructuredTool.from_function(
            name="import_pipeline",
            description="Import a pipeline bundle. conflict_strategy: create (default) or reuse.",
            func=lambda bundle, name=None, conflict_strategy="create": _safe(
                ops.import_pipeline, bundle, name, conflict_strategy
            ),
        ),
        StructuredTool.from_function(
            name="list_executions",
            description="List execution history for a pipeline.",
            func=lambda pipeline_id: _safe(ops.list_executions, pipeline_id),
        ),
        StructuredTool.from_function(
            name="get_execution",
            description="Get execution detail including per-step status.",
            func=lambda pipeline_id, execution_id: _safe(ops.get_execution, pipeline_id, execution_id),
        ),
        StructuredTool.from_function(
            name="cancel_execution",
            description="Cancel a running or pending execution.",
            func=lambda pipeline_id, execution_id: _safe(ops.cancel_execution, pipeline_id, execution_id),
        ),
        StructuredTool.from_function(
            name="get_execution_logs",
            description="Fetch indexed execution logs from observability.",
            func=lambda execution_id: _safe(ops.get_execution_logs, execution_id),
        ),
        StructuredTool.from_function(
            name="debug_execution",
            description="Investigate an execution: detail, logs, errors, completeness, and portal links.",
            func=lambda pipeline_id, execution_id: _safe(ops.debug_execution, pipeline_id, execution_id),
        ),
        StructuredTool.from_function(
            name="get_pipeline_observability",
            description="Fetch completeness, latency, heartbeat, and errors for a pipeline.",
            func=lambda pipeline_id: _safe(ops.get_pipeline_observability, pipeline_id),
        ),
        StructuredTool.from_function(
            name="get_observability_links",
            description="Get Grafana/Elasticsearch portal links when configured.",
            func=lambda pipeline_id=None, execution_id=None: _safe(
                ops.get_observability_links, pipeline_id, execution_id
            ),
        ),
        StructuredTool.from_function(
            name="list_connector_types",
            description="List global connector SPI types.",
            func=lambda: _safe(ops.list_connector_types),
        ),
        StructuredTool.from_function(
            name="list_connectors",
            description="List tenant connector instances.",
            func=lambda: _safe(ops.list_connectors),
        ),
        StructuredTool.from_function(
            name="get_connector",
            description="Get a tenant connector by id.",
            func=lambda connector_id: _safe(ops.get_connector, connector_id),
        ),
        StructuredTool.from_function(
            name="create_connector",
            description="Create a tenant connector instance.",
            func=lambda connector_type_id, name, config=None, deployment_config=None, execution_config=None: _safe(
                ops.create_connector, connector_type_id, name, config, deployment_config, execution_config
            ),
        ),
        StructuredTool.from_function(
            name="update_connector",
            description="Update a tenant connector.",
            func=lambda connector_id, name, config=None, deployment_config=None, execution_config=None, status=None: _safe(
                ops.update_connector, connector_id, name, config, deployment_config, execution_config, status
            ),
        ),
        StructuredTool.from_function(
            name="test_connector",
            description="Run a connection test against a tenant connector.",
            func=lambda connector_id: _safe(ops.test_connector, connector_id),
        ),
        StructuredTool.from_function(
            name="provision_connector_webhook",
            description="Provision a webhook URL and signing secret for an event-listener connector.",
            func=lambda connector_id: _safe(ops.provision_connector_webhook, connector_id),
        ),
        StructuredTool.from_function(
            name="list_pipelets",
            description="Browse the pipelet catalog (Source, Processor, Destination).",
            func=lambda category=None, active_only=False, query=None, limit=50: _safe(
                ops.list_pipelets, category, active_only, query, limit
            ),
        ),
        StructuredTool.from_function(
            name="get_pipelet",
            description="Get a pipelet catalog entry by id.",
            func=lambda pipelet_id: _safe(ops.get_pipelet, pipelet_id),
        ),
    ]
