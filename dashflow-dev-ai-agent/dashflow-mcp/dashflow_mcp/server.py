from __future__ import annotations

import json
from typing import Any

from fastmcp import FastMCP

from dashflow_mcp.catalog import PipeletCatalog
from dashflow_mcp.client import DashflowApiError, DashflowClient
from dashflow_mcp.config import Settings, settings

mcp = FastMCP(
    name="dashflow",
    instructions=(
        "Tools for the Dashflow data pipeline control plane. "
        "Use these to list/create/update pipelines, run and debug executions, "
        "import/export bundles, manage tenant connectors, and browse the pipelet catalog. "
        "Most operations require dashflow-api running locally (default http://localhost:8080) "
        "with X-Tenant-Id (default T001)."
    ),
)

_client: DashflowClient | None = None
_catalog: PipeletCatalog | None = None


def _client() -> DashflowClient:
    global _client
    if _client is None:
        _client = DashflowClient(settings)
    return _client


def _catalog() -> PipeletCatalog:
    global _catalog
    if _catalog is None:
        path = settings.resolve_pipelets_catalog()
        _catalog = PipeletCatalog(path)
    return _catalog


def _ok(data: Any) -> str:
    return json.dumps(data, indent=2, default=str)


def _err(exc: Exception) -> str:
    if isinstance(exc, DashflowApiError):
        return json.dumps({"error": exc.detail, "status_code": exc.status_code}, indent=2)
    return json.dumps({"error": str(exc)}, indent=2)


# --- meta / health ---


@mcp.tool
def dashflow_health() -> str:
    """Check dashflow-api actuator health (no tenant header required)."""
    try:
        return _ok(_client().health())
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def dashflow_config() -> str:
    """Show effective MCP client configuration (API URL, tenant, pipelet catalog path)."""
    cfg = settings
    return _ok(
        {
            "api_url": cfg.api_url,
            "tenant_id": cfg.tenant_id,
            "pipelets_catalog": str(cfg.resolve_pipelets_catalog()),
            "request_timeout_s": cfg.request_timeout_s,
        }
    )


# --- pipelines ---


@mcp.tool
def list_pipelines() -> str:
    """List all pipelines for the configured tenant."""
    try:
        return _ok(_client().list_pipelines())
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def get_pipeline(pipeline_id: str) -> str:
    """Get a pipeline by id including its steps."""
    try:
        return _ok(_client().get_pipeline(pipeline_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


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
    body = {
        "name": name,
        "description": description or None,
        "visibility": visibility.upper() if visibility else "PRIVATE",
        "executionMode": execution_mode.upper() if execution_mode else "ASYNC",
        "deployment_config": deployment_config or {},
        "execution_config": execution_config or {},
    }
    try:
        return _ok(_client().create_pipeline(body))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


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
    body = {
        "name": name,
        "description": description or None,
        "visibility": visibility.upper() if visibility else "PRIVATE",
        "executionMode": execution_mode.upper() if execution_mode else "ASYNC",
        "status": status.upper() if status else "DRAFT",
        "deployment_config": deployment_config or {},
        "execution_config": execution_config or {},
    }
    try:
        return _ok(_client().update_pipeline(pipeline_id, body))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def activate_pipeline(pipeline_id: str) -> str:
    """Activate a pipeline (sets status to active; required before run)."""
    try:
        current = _client().get_pipeline(pipeline_id)
        body = {
            "name": current["name"],
            "description": current.get("description"),
            "visibility": str(current.get("visibility", "PRIVATE")).upper(),
            "executionMode": str(current.get("execution_mode", "ASYNC")).upper(),
            "status": "ACTIVE",
            "deployment_config": current.get("deployment_config") or {},
            "execution_config": current.get("execution_config") or {},
        }
        return _ok(_client().update_pipeline(pipeline_id, body))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def deactivate_pipeline(pipeline_id: str) -> str:
    """Deactivate a pipeline (sets status back to draft)."""
    try:
        current = _client().get_pipeline(pipeline_id)
        body = {
            "name": current["name"],
            "description": current.get("description"),
            "visibility": str(current.get("visibility", "PRIVATE")).upper(),
            "executionMode": str(current.get("execution_mode", "ASYNC")).upper(),
            "status": "DRAFT",
            "deployment_config": current.get("deployment_config") or {},
            "execution_config": current.get("execution_config") or {},
        }
        return _ok(_client().update_pipeline(pipeline_id, body))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def archive_pipeline(pipeline_id: str) -> str:
    """Archive (soft-delete) a pipeline."""
    try:
        return _ok(_client().archive_pipeline(pipeline_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def replace_pipeline_steps(pipeline_id: str, steps: list[dict[str, Any]]) -> str:
    """Replace the full step graph for a pipeline. Each step needs pipelet_id and step_order."""
    try:
        return _ok(_client().replace_pipeline_steps(pipeline_id, steps))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def dry_run_pipeline(pipeline_id: str) -> str:
    """Validate a pipeline without executing it."""
    try:
        return _ok(_client().dry_run_pipeline(pipeline_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def run_pipeline(pipeline_id: str, payload: dict[str, Any] | None = None) -> str:
    """Start a pipeline execution. Pipeline must be active."""
    try:
        return _ok(_client().run_pipeline(pipeline_id, payload))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def export_pipeline(pipeline_id: str) -> str:
    """Export a pipeline bundle (pipeline, steps, connectors, services) for portability."""
    try:
        return _ok(_client().export_pipeline(pipeline_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def import_pipeline(
    bundle: dict[str, Any],
    name: str | None = None,
    conflict_strategy: str = "create",
) -> str:
    """Import a pipeline bundle. conflict_strategy: create (default) or reuse."""
    body: dict[str, Any] = {"bundle": bundle, "conflict_strategy": conflict_strategy}
    if name:
        body["name"] = name
    try:
        return _ok(_client().import_pipeline(body))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


# --- executions ---


@mcp.tool
def list_executions(pipeline_id: str) -> str:
    """List execution history for a pipeline."""
    try:
        return _ok(_client().list_executions(pipeline_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def get_execution(pipeline_id: str, execution_id: str) -> str:
    """Get execution detail including per-step status."""
    try:
        return _ok(_client().get_execution(pipeline_id, execution_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def cancel_execution(pipeline_id: str, execution_id: str) -> str:
    """Cancel a running or pending execution."""
    try:
        return _ok(_client().cancel_execution(pipeline_id, execution_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def get_execution_logs(execution_id: str) -> str:
    """Fetch indexed execution logs from observability."""
    try:
        return _ok(_client().execution_logs(execution_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def debug_execution(pipeline_id: str, execution_id: str) -> str:
    """Investigate an execution: detail, logs, errors, completeness, and portal links."""
    try:
        client = _client()
        result = {
            "execution": client.get_execution(pipeline_id, execution_id),
            "logs": client.execution_logs(execution_id),
            "pipeline_errors": client.pipeline_errors(pipeline_id),
            "completeness": client.pipeline_completeness(pipeline_id),
            "links": client.observability_links(pipeline_id, execution_id),
        }
        return _ok(result)
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


# --- observability ---


@mcp.tool
def get_pipeline_observability(pipeline_id: str) -> str:
    """Fetch completeness, latency, heartbeat, and errors for a pipeline."""
    try:
        client = _client()
        return _ok(
            {
                "completeness": client.pipeline_completeness(pipeline_id),
                "latency": client.pipeline_latency(pipeline_id),
                "heartbeat": client.pipeline_heartbeat(pipeline_id),
                "errors": client.pipeline_errors(pipeline_id),
            }
        )
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def get_observability_links(
    pipeline_id: str | None = None, execution_id: str | None = None
) -> str:
    """Get Grafana/Elasticsearch portal links when configured."""
    try:
        return _ok(_client().observability_links(pipeline_id, execution_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


# --- connectors ---


@mcp.tool
def list_connector_types() -> str:
    """List global connector SPI types (REST, storage, message bus, etc.)."""
    try:
        return _ok(_client().list_connector_types())
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def list_connectors() -> str:
    """List tenant connector instances."""
    try:
        return _ok(_client().list_connectors())
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def get_connector(connector_id: str) -> str:
    """Get a tenant connector by id."""
    try:
        return _ok(_client().get_connector(connector_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def create_connector(
    connector_type_id: str,
    name: str,
    config: dict[str, Any] | None = None,
    deployment_config: dict[str, Any] | None = None,
    execution_config: dict[str, Any] | None = None,
) -> str:
    """Create a tenant connector instance."""
    body = {
        "connectorTypeId": connector_type_id,
        "name": name,
        "config": config or {},
        "deployment_config": deployment_config or {},
        "execution_config": execution_config or {},
    }
    try:
        return _ok(_client().create_connector(body))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


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
    body: dict[str, Any] = {
        "name": name,
        "config": config or {},
        "deployment_config": deployment_config or {},
        "execution_config": execution_config or {},
    }
    if status:
        body["status"] = status
    try:
        return _ok(_client().update_connector(connector_id, body))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def test_connector(connector_id: str) -> str:
    """Run a connection test against a tenant connector."""
    try:
        return _ok(_client().test_connector(connector_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def provision_connector_webhook(connector_id: str) -> str:
    """Provision a webhook URL and signing secret for an event-listener connector."""
    try:
        return _ok(_client().provision_webhook_url(connector_id))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


# --- pipelets (catalog) ---


@mcp.tool
def list_pipelets(
    category: str | None = None,
    active_only: bool = False,
    query: str | None = None,
    limit: int = 50,
) -> str:
    """Browse the pipelet catalog (Source, Processor, Destination). category is case-insensitive."""
    try:
        return _ok(_catalog().list_pipelets(category=category, active_only=active_only, query=query, limit=limit))
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


@mcp.tool
def get_pipelet(pipelet_id: str) -> str:
    """Get a pipelet catalog entry by id (e.g. plet-rest-source)."""
    try:
        pipelet = _catalog().get_pipelet(pipelet_id)
        if pipelet is None:
            return _err(ValueError(f"Pipelet not found: {pipelet_id}"))
        return _ok(pipelet)
    except Exception as exc:  # noqa: BLE001
        return _err(exc)


def run(cfg: Settings | None = None) -> None:
    global settings, _client, _catalog
    if cfg is not None:
        settings = cfg
        _client = None
        _catalog = None
    mcp.run()


if __name__ == "__main__":
    run()
