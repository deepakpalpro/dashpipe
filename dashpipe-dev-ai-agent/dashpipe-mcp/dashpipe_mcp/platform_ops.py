"""Shared platform operations — used by MCP server and dev-agent LangChain tools."""

from __future__ import annotations

from typing import Any

from dashpipe_mcp.catalog import PipeletCatalog
from dashpipe_mcp.client import DashpipeClient
from dashpipe_mcp.config import Settings


class PlatformOps:
    def __init__(self, cfg: Settings | None = None):
        self.cfg = cfg or Settings()
        self._client: DashpipeClient | None = None
        self._catalog: PipeletCatalog | None = None

    @property
    def client(self) -> DashpipeClient:
        if self._client is None:
            self._client = DashpipeClient(self.cfg)
        return self._client

    @property
    def catalog(self) -> PipeletCatalog:
        if self._catalog is None:
            self._catalog = PipeletCatalog(self.cfg.resolve_pipelets_catalog())
        return self._catalog

    def close(self) -> None:
        if self._client is not None:
            self._client.close()
            self._client = None

    def dashpipe_health(self) -> dict[str, Any]:
        return self.client.health()

    def dashpipe_config(self) -> dict[str, Any]:
        return {
            "api_url": self.cfg.api_url,
            "tenant_id": self.cfg.tenant_id,
            "pipelets_catalog": str(self.cfg.resolve_pipelets_catalog()),
            "request_timeout_s": self.cfg.request_timeout_s,
        }

    def list_pipelines(self) -> list[dict[str, Any]]:
        return self.client.list_pipelines()

    def get_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self.client.get_pipeline(pipeline_id)

    def create_pipeline(
        self,
        name: str,
        description: str = "",
        visibility: str = "private",
        execution_mode: str = "async",
        deployment_config: dict[str, Any] | None = None,
        execution_config: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        body = {
            "name": name,
            "description": description or None,
            "visibility": visibility.upper() if visibility else "PRIVATE",
            "executionMode": execution_mode.upper() if execution_mode else "ASYNC",
            "deployment_config": deployment_config or {},
            "execution_config": execution_config or {},
        }
        return self.client.create_pipeline(body)

    def update_pipeline(
        self,
        pipeline_id: str,
        name: str,
        description: str = "",
        visibility: str = "private",
        execution_mode: str = "async",
        status: str = "draft",
        deployment_config: dict[str, Any] | None = None,
        execution_config: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        body = {
            "name": name,
            "description": description or None,
            "visibility": visibility.upper() if visibility else "PRIVATE",
            "executionMode": execution_mode.upper() if execution_mode else "ASYNC",
            "status": status.upper() if status else "DRAFT",
            "deployment_config": deployment_config or {},
            "execution_config": execution_config or {},
        }
        return self.client.update_pipeline(pipeline_id, body)

    def _status_update(self, pipeline_id: str, status: str) -> dict[str, Any]:
        current = self.client.get_pipeline(pipeline_id)
        body = {
            "name": current["name"],
            "description": current.get("description"),
            "visibility": str(current.get("visibility", "PRIVATE")).upper(),
            "executionMode": str(current.get("execution_mode", "ASYNC")).upper(),
            "status": status,
            "deployment_config": current.get("deployment_config") or {},
            "execution_config": current.get("execution_config") or {},
        }
        return self.client.update_pipeline(pipeline_id, body)

    def activate_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self._status_update(pipeline_id, "ACTIVE")

    def deactivate_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self._status_update(pipeline_id, "DRAFT")

    def archive_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self.client.archive_pipeline(pipeline_id)

    def replace_pipeline_steps(self, pipeline_id: str, steps: list[dict[str, Any]]) -> dict[str, Any]:
        return self.client.replace_pipeline_steps(pipeline_id, steps)

    def dry_run_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self.client.dry_run_pipeline(pipeline_id)

    def run_pipeline(self, pipeline_id: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
        return self.client.run_pipeline(pipeline_id, payload)

    def export_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self.client.export_pipeline(pipeline_id)

    def import_pipeline(
        self,
        bundle: dict[str, Any],
        name: str | None = None,
        conflict_strategy: str = "create",
    ) -> dict[str, Any]:
        body: dict[str, Any] = {"bundle": bundle, "conflict_strategy": conflict_strategy}
        if name:
            body["name"] = name
        return self.client.import_pipeline(body)

    def list_executions(self, pipeline_id: str) -> list[dict[str, Any]]:
        return self.client.list_executions(pipeline_id)

    def get_execution(self, pipeline_id: str, execution_id: str) -> dict[str, Any]:
        return self.client.get_execution(pipeline_id, execution_id)

    def cancel_execution(self, pipeline_id: str, execution_id: str) -> dict[str, Any]:
        return self.client.cancel_execution(pipeline_id, execution_id)

    def get_execution_logs(self, execution_id: str) -> dict[str, Any]:
        return self.client.execution_logs(execution_id)

    def debug_execution(self, pipeline_id: str, execution_id: str) -> dict[str, Any]:
        client = self.client
        return {
            "execution": client.get_execution(pipeline_id, execution_id),
            "logs": client.execution_logs(execution_id),
            "pipeline_errors": client.pipeline_errors(pipeline_id),
            "completeness": client.pipeline_completeness(pipeline_id),
            "links": client.observability_links(pipeline_id, execution_id),
        }

    def get_pipeline_observability(self, pipeline_id: str) -> dict[str, Any]:
        client = self.client
        return {
            "completeness": client.pipeline_completeness(pipeline_id),
            "latency": client.pipeline_latency(pipeline_id),
            "heartbeat": client.pipeline_heartbeat(pipeline_id),
            "errors": client.pipeline_errors(pipeline_id),
        }

    def get_observability_links(
        self, pipeline_id: str | None = None, execution_id: str | None = None
    ) -> dict[str, Any]:
        return self.client.observability_links(pipeline_id, execution_id)

    def list_connector_types(self) -> list[dict[str, Any]]:
        return self.client.list_connector_types()

    def list_connectors(self) -> list[dict[str, Any]]:
        return self.client.list_connectors()

    def get_connector(self, connector_id: str) -> dict[str, Any]:
        return self.client.get_connector(connector_id)

    def create_connector(
        self,
        connector_type_id: str,
        name: str,
        config: dict[str, Any] | None = None,
        deployment_config: dict[str, Any] | None = None,
        execution_config: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        body = {
            "connectorTypeId": connector_type_id,
            "name": name,
            "config": config or {},
            "deployment_config": deployment_config or {},
            "execution_config": execution_config or {},
        }
        return self.client.create_connector(body)

    def update_connector(
        self,
        connector_id: str,
        name: str,
        config: dict[str, Any] | None = None,
        deployment_config: dict[str, Any] | None = None,
        execution_config: dict[str, Any] | None = None,
        status: str | None = None,
    ) -> dict[str, Any]:
        body: dict[str, Any] = {
            "name": name,
            "config": config or {},
            "deployment_config": deployment_config or {},
            "execution_config": execution_config or {},
        }
        if status:
            body["status"] = status
        return self.client.update_connector(connector_id, body)

    def test_connector(self, connector_id: str) -> dict[str, Any]:
        return self.client.test_connector(connector_id)

    def provision_connector_webhook(self, connector_id: str) -> dict[str, Any]:
        return self.client.provision_webhook_url(connector_id)

    def list_pipelets(
        self,
        category: str | None = None,
        active_only: bool = False,
        query: str | None = None,
        limit: int = 50,
    ) -> list[dict[str, Any]]:
        return self.catalog.list_pipelets(
            category=category, active_only=active_only, query=query, limit=limit
        )

    def get_pipelet(self, pipelet_id: str) -> dict[str, Any]:
        pipelet = self.catalog.get_pipelet(pipelet_id)
        if pipelet is None:
            raise ValueError(f"Pipelet not found: {pipelet_id}")
        return pipelet
