from __future__ import annotations

from typing import Any

import httpx

from dashpipe_mcp.config import Settings


class DashpipeApiError(Exception):
    def __init__(self, status_code: int, detail: str):
        self.status_code = status_code
        self.detail = detail
        super().__init__(f"HTTP {status_code}: {detail}")


class DashpipeClient:
    def __init__(self, cfg: Settings | None = None):
        self.cfg = cfg or Settings()
        self._client = httpx.Client(
            base_url=self.cfg.api_url.rstrip("/"),
            timeout=self.cfg.request_timeout_s,
        )

    def close(self) -> None:
        self._client.close()

    def _request(
        self,
        method: str,
        path: str,
        *,
        json: dict[str, Any] | list[Any] | None = None,
        params: dict[str, str] | None = None,
        tenant_scoped: bool = True,
    ) -> Any:
        headers = {"Accept": "application/json", "Content-Type": "application/json"}
        if tenant_scoped:
            headers["X-Tenant-Id"] = self.cfg.tenant_id
        response = self._client.request(method, path, json=json, params=params, headers=headers)
        if response.status_code >= 400:
            detail = response.text.strip() or response.reason_phrase
            raise DashpipeApiError(response.status_code, detail)
        if response.status_code == 204 or not response.content:
            return None
        return response.json()

    # --- health ---

    def health(self) -> dict[str, Any]:
        return self._request("GET", "/actuator/health", tenant_scoped=False)

    # --- pipelines ---

    def list_pipelines(self) -> list[dict[str, Any]]:
        return self._request("GET", "/api/v1/pipelines")

    def get_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self._request("GET", f"/api/v1/pipelines/{pipeline_id}")

    def create_pipeline(self, body: dict[str, Any]) -> dict[str, Any]:
        return self._request("POST", "/api/v1/pipelines", json=body)

    def update_pipeline(self, pipeline_id: str, body: dict[str, Any]) -> dict[str, Any]:
        return self._request("PUT", f"/api/v1/pipelines/{pipeline_id}", json=body)

    def archive_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self._request("DELETE", f"/api/v1/pipelines/{pipeline_id}")

    def replace_pipeline_steps(self, pipeline_id: str, steps: list[dict[str, Any]]) -> dict[str, Any]:
        return self._request("PUT", f"/api/v1/pipelines/{pipeline_id}/steps", json={"steps": steps})

    def dry_run_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self._request("POST", f"/api/v1/pipelines/{pipeline_id}/dry-run")

    def run_pipeline(self, pipeline_id: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
        body = {"payload": payload} if payload is not None else None
        return self._request("POST", f"/api/v1/pipelines/{pipeline_id}/run", json=body)

    def export_pipeline(self, pipeline_id: str) -> dict[str, Any]:
        return self._request("GET", f"/api/v1/pipelines/{pipeline_id}/export")

    def import_pipeline(self, body: dict[str, Any]) -> dict[str, Any]:
        return self._request("POST", "/api/v1/pipelines/import", json=body)

    # --- executions ---

    def list_executions(self, pipeline_id: str) -> list[dict[str, Any]]:
        return self._request("GET", f"/api/v1/pipelines/{pipeline_id}/executions")

    def get_execution(self, pipeline_id: str, execution_id: str) -> dict[str, Any]:
        return self._request("GET", f"/api/v1/pipelines/{pipeline_id}/executions/{execution_id}")

    def cancel_execution(self, pipeline_id: str, execution_id: str) -> dict[str, Any]:
        return self._request(
            "POST", f"/api/v1/pipelines/{pipeline_id}/executions/{execution_id}/cancel"
        )

    # --- observability ---

    def execution_logs(self, execution_id: str) -> dict[str, Any]:
        return self._request("GET", f"/api/v1/observability/executions/{execution_id}/logs")

    def pipeline_completeness(self, pipeline_id: str) -> dict[str, Any]:
        return self._request("GET", f"/api/v1/observability/pipelines/{pipeline_id}/completeness")

    def pipeline_latency(self, pipeline_id: str) -> dict[str, Any]:
        return self._request("GET", f"/api/v1/observability/pipelines/{pipeline_id}/latency")

    def pipeline_heartbeat(self, pipeline_id: str) -> dict[str, Any]:
        return self._request("GET", f"/api/v1/observability/pipelines/{pipeline_id}/heartbeat")

    def pipeline_errors(self, pipeline_id: str) -> dict[str, Any]:
        return self._request("GET", f"/api/v1/observability/pipelines/{pipeline_id}/errors")

    def observability_links(
        self, pipeline_id: str | None = None, execution_id: str | None = None
    ) -> dict[str, Any]:
        params: dict[str, str] = {}
        if pipeline_id:
            params["pipelineId"] = pipeline_id
        if execution_id:
            params["executionId"] = execution_id
        return self._request("GET", "/api/v1/observability/links", params=params or None)

    # --- connectors ---

    def list_connector_types(self) -> list[dict[str, Any]]:
        return self._request("GET", "/api/v1/connector-types", tenant_scoped=False)

    def list_connectors(self) -> list[dict[str, Any]]:
        return self._request("GET", "/api/v1/connectors")

    def get_connector(self, connector_id: str) -> dict[str, Any]:
        return self._request("GET", f"/api/v1/connectors/{connector_id}")

    def create_connector(self, body: dict[str, Any]) -> dict[str, Any]:
        return self._request("POST", "/api/v1/connectors", json=body)

    def update_connector(self, connector_id: str, body: dict[str, Any]) -> dict[str, Any]:
        return self._request("PUT", f"/api/v1/connectors/{connector_id}", json=body)

    def test_connector(self, connector_id: str) -> dict[str, Any]:
        return self._request("POST", f"/api/v1/connectors/{connector_id}/test")

    def provision_webhook_url(self, connector_id: str) -> dict[str, Any]:
        return self._request("POST", f"/api/v1/connectors/{connector_id}/webhook-url")
