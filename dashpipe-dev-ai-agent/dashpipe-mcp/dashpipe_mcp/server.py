from __future__ import annotations

import json
from collections.abc import Callable
from typing import Any

from fastmcp import FastMCP

from dashpipe_mcp.client import DashpipeApiError
from dashpipe_mcp.config import Settings, settings
from dashpipe_mcp.platform_ops import PlatformOps

mcp = FastMCP(
    name="dashpipe",
    instructions=(
        "Tools for the Dashpipe data pipeline control plane. "
        "Use these to list/create/update pipelines, run and debug executions, "
        "import/export bundles, manage tenant connectors, and browse the pipelet catalog. "
        "Most operations require dashpipe-api running locally (default http://localhost:8080) "
        "with X-Tenant-Id (default T001)."
    ),
)

_ops: PlatformOps | None = None


def _get_ops() -> PlatformOps:
    global _ops
    if _ops is None:
        _ops = PlatformOps(settings)
    return _ops


def _ok(data: Any) -> str:
    return json.dumps(data, indent=2, default=str)


def _err(exc: Exception) -> str:
    if isinstance(exc, DashpipeApiError):
        return json.dumps({"error": exc.detail, "status_code": exc.status_code}, indent=2)
    return json.dumps({"error": str(exc)}, indent=2)


def _wrap(fn: Callable[..., Any], *args: Any, **kwargs: Any) -> str:
    try:
        return _ok(fn(*args, **kwargs))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


# --- meta / health ---


@mcp.tool
def dashpipe_health() -> str:
    """Check dashpipe-api actuator health (no tenant header required)."""
    return _wrap(_get_ops().dashpipe_health)


@mcp.tool
def dashpipe_config() -> str:
    """Show effective MCP client configuration (API URL, tenant, pipelet catalog path)."""
    return _wrap(_get_ops().dashpipe_config)


# --- pipelines ---


@mcp.tool
def list_pipelines() -> str:
    """List all pipelines for the configured tenant."""
    return _wrap(_get_ops().list_pipelines)


@mcp.tool
def get_pipeline(pipeline_id: str) -> str:
    """Get a pipeline by id including its steps."""
    return _wrap(_get_ops().get_pipeline, pipeline_id)


@mcp.tool
def create_pipeline(
    name: str,
    description: str = "",
    visibility: str = "private",
    execution_mode: str = "async",
    deployment_config: dict[str, Any] | None = None,
    execution_config: dict[str, Any] | None = None,
) -> str:
    """Create a new pipeline in draft status."""
    return _wrap(
        _get_ops().create_pipeline,
        name,
        description,
        visibility,
        execution_mode,
        deployment_config,
        execution_config,
    )


@mcp.tool
def update_pipeline(
    pipeline_id: str,
    name: str,
    description: str = "",
    visibility: str = "private",
    execution_mode: str = "async",
    status: str = "draft",
    deployment_config: dict[str, Any] | None = None,
    execution_config: dict[str, Any] | None = None,
) -> str:
    """Update pipeline metadata and status (draft | active | archived via archive_pipeline)."""
    return _wrap(
        _get_ops().update_pipeline,
        pipeline_id,
        name,
        description,
        visibility,
        execution_mode,
        status,
        deployment_config,
        execution_config,
    )


@mcp.tool
def activate_pipeline(pipeline_id: str) -> str:
    """Activate a pipeline (sets status to active; required before run)."""
    return _wrap(_get_ops().activate_pipeline, pipeline_id)


@mcp.tool
def deactivate_pipeline(pipeline_id: str) -> str:
    """Deactivate a pipeline (sets status back to draft)."""
    return _wrap(_get_ops().deactivate_pipeline, pipeline_id)


@mcp.tool
def archive_pipeline(pipeline_id: str) -> str:
    """Archive (soft-delete) a pipeline."""
    return _wrap(_get_ops().archive_pipeline, pipeline_id)


@mcp.tool
def replace_pipeline_steps(pipeline_id: str, steps: list[dict[str, Any]]) -> str:
    """Replace the full step graph for a pipeline. Each step needs pipelet_id and step_order."""
    return _wrap(_get_ops().replace_pipeline_steps, pipeline_id, steps)


@mcp.tool
def dry_run_pipeline(pipeline_id: str) -> str:
    """Validate a pipeline without executing it."""
    return _wrap(_get_ops().dry_run_pipeline, pipeline_id)


@mcp.tool
def run_pipeline(pipeline_id: str, payload: dict[str, Any] | None = None) -> str:
    """Start a pipeline execution. Pipeline must be active."""
    return _wrap(_get_ops().run_pipeline, pipeline_id, payload)


@mcp.tool
def export_pipeline(pipeline_id: str) -> str:
    """Export a pipeline bundle (pipeline, steps, connectors, services) for portability."""
    return _wrap(_get_ops().export_pipeline, pipeline_id)


@mcp.tool
def import_pipeline(
    bundle: dict[str, Any],
    name: str | None = None,
    conflict_strategy: str = "create",
) -> str:
    """Import a pipeline bundle. conflict_strategy: create (default) or reuse."""
    return _wrap(_get_ops().import_pipeline, bundle, name, conflict_strategy)


# --- executions ---


@mcp.tool
def list_executions(pipeline_id: str) -> str:
    """List execution history for a pipeline."""
    return _wrap(_get_ops().list_executions, pipeline_id)


@mcp.tool
def get_execution(pipeline_id: str, execution_id: str) -> str:
    """Get execution detail including per-step status."""
    return _wrap(_get_ops().get_execution, pipeline_id, execution_id)


@mcp.tool
def cancel_execution(pipeline_id: str, execution_id: str) -> str:
    """Cancel a running or pending execution."""
    return _wrap(_get_ops().cancel_execution, pipeline_id, execution_id)


@mcp.tool
def get_execution_logs(execution_id: str) -> str:
    """Fetch indexed execution logs from observability."""
    return _wrap(_get_ops().get_execution_logs, execution_id)


@mcp.tool
def debug_execution(pipeline_id: str, execution_id: str) -> str:
    """Investigate an execution: detail, logs, errors, completeness, and portal links."""
    return _wrap(_get_ops().debug_execution, pipeline_id, execution_id)


# --- observability ---


@mcp.tool
def get_pipeline_observability(pipeline_id: str) -> str:
    """Fetch completeness, latency, heartbeat, and errors for a pipeline."""
    return _wrap(_get_ops().get_pipeline_observability, pipeline_id)


@mcp.tool
def get_observability_links(
    pipeline_id: str | None = None, execution_id: str | None = None
) -> str:
    """Get Grafana/Elasticsearch portal links when configured."""
    return _wrap(_get_ops().get_observability_links, pipeline_id, execution_id)


# --- connectors ---


@mcp.tool
def list_connector_types() -> str:
    """List global connector SPI types (REST, storage, message bus, etc.)."""
    return _wrap(_get_ops().list_connector_types)


@mcp.tool
def list_connectors() -> str:
    """List tenant connector instances."""
    return _wrap(_get_ops().list_connectors)


@mcp.tool
def get_connector(connector_id: str) -> str:
    """Get a tenant connector by id."""
    return _wrap(_get_ops().get_connector, connector_id)


@mcp.tool
def create_connector(
    connector_type_id: str,
    name: str,
    config: dict[str, Any] | None = None,
    deployment_config: dict[str, Any] | None = None,
    execution_config: dict[str, Any] | None = None,
) -> str:
    """Create a tenant connector instance."""
    return _wrap(
        _get_ops().create_connector,
        connector_type_id,
        name,
        config,
        deployment_config,
        execution_config,
    )


@mcp.tool
def update_connector(
    connector_id: str,
    name: str,
    config: dict[str, Any] | None = None,
    deployment_config: dict[str, Any] | None = None,
    execution_config: dict[str, Any] | None = None,
    status: str | None = None,
) -> str:
    """Update a tenant connector (status: active | inactive | error)."""
    return _wrap(
        _get_ops().update_connector,
        connector_id,
        name,
        config,
        deployment_config,
        execution_config,
        status,
    )


@mcp.tool
def test_connector(connector_id: str) -> str:
    """Run a connection test against a tenant connector."""
    return _wrap(_get_ops().test_connector, connector_id)


@mcp.tool
def provision_connector_webhook(connector_id: str) -> str:
    """Provision a webhook URL and signing secret for an event-listener connector."""
    return _wrap(_get_ops().provision_connector_webhook, connector_id)


# --- pipelets (catalog) ---


@mcp.tool
def list_pipelets(
    category: str | None = None,
    active_only: bool = False,
    query: str | None = None,
    limit: int = 50,
) -> str:
    """Browse the pipelet catalog (Source, Processor, Destination). category is case-insensitive."""
    return _wrap(_get_ops().list_pipelets, category, active_only, query, limit)


@mcp.tool
def get_pipelet(pipelet_id: str) -> str:
    """Get a pipelet catalog entry by id (e.g. plet-rest-source)."""
    return _wrap(_get_ops().get_pipelet, pipelet_id)


def run(cfg: Settings | None = None) -> None:
    global settings, _ops
    if cfg is not None:
        settings = cfg
        _ops = None
    mcp.run()


if __name__ == "__main__":
    run()
