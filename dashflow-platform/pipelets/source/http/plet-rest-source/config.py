"""
plet-rest-source config merge.

Required after merge:
  deployment.region
  execution.path
  connector.baseUrl
"""

from __future__ import annotations

import json
import os
from typing import Any

REQUIRED_DEPLOYMENT_KEYS = ("region",)
REQUIRED_EXECUTION_KEYS = ("path",)


def load_json_env(name: str, default: dict[str, Any] | None = None) -> dict[str, Any]:
    raw = os.environ.get(name)
    if not raw or not raw.strip():
        return dict(default or {})
    value = json.loads(raw)
    if not isinstance(value, dict):
        raise SystemExit(f"{name} must be a JSON object")
    return value


def shallow_merge(*layers: dict[str, Any] | None) -> dict[str, Any]:
    out: dict[str, Any] = {}
    for layer in layers:
        if not layer:
            continue
        for key, value in layer.items():
            if value is None:
                continue
            if isinstance(value, str) and value.strip() == "":
                if key not in out:
                    out[key] = value
                continue
            out[key] = value
    return out


def _first_non_empty(*values: Any) -> Any:
    for value in values:
        if value is None:
            continue
        if isinstance(value, str) and value.strip() == "":
            continue
        return value
    return None


class ResolvedRestSourceConfig:
    def __init__(
        self,
        *,
        base_url: str,
        path: str,
        method: str,
        timeout_sec: float,
        headers: dict[str, str],
        query: dict[str, str],
        body: Any | None,
        deployment: dict[str, Any],
        execution: dict[str, Any],
        connector: dict[str, Any],
    ) -> None:
        self.base_url = base_url
        self.path = path
        self.method = method
        self.timeout_sec = timeout_sec
        self.headers = headers
        self.query = query
        self.body = body
        self.deployment = deployment
        self.execution = execution
        self.connector = connector

    @property
    def url(self) -> str:
        base = self.base_url.rstrip("/")
        path = self.path if self.path.startswith("/") else "/" + self.path
        if path == "/":
            return base
        return base + path


def _as_str_map(value: Any) -> dict[str, str]:
    if not isinstance(value, dict):
        return {}
    out: dict[str, str] = {}
    for k, v in value.items():
        if v is None:
            continue
        out[str(k)] = str(v)
    return out


def resolve_config(
    *,
    connector: dict[str, Any] | None = None,
    service: dict[str, Any] | None = None,
    deployment: dict[str, Any] | None = None,
    execution: dict[str, Any] | None = None,
    pipelet_deployment_defaults: dict[str, Any] | None = None,
    pipelet_execution_defaults: dict[str, Any] | None = None,
) -> ResolvedRestSourceConfig:
    connector = connector or {}
    service = service or {}

    connector_deployment = connector.get("deployment_config") or connector.get(
        "deploymentConfiguration"
    )
    connector_execution = (
        connector.get("execution_config")
        or connector.get("executionConfiguration")
        or connector.get("config")
    )
    if not isinstance(connector_deployment, dict):
        connector_deployment = {}
    if not isinstance(connector_execution, dict):
        connector_execution = {
            k: v
            for k, v in connector.items()
            if k
            not in (
                "deployment_config",
                "deploymentConfiguration",
                "execution_config",
                "executionConfiguration",
                "config",
            )
        }

    service_execution = (
        service.get("execution_config")
        or service.get("tenant_config")
        or service.get("executionConfiguration")
        or service
    )
    if not isinstance(service_execution, dict):
        service_execution = {}

    merged_deployment = shallow_merge(
        pipelet_deployment_defaults,
        connector_deployment,
        deployment,
    )
    merged_execution = shallow_merge(
        pipelet_execution_defaults,
        connector_execution,
        execution,
    )

    missing: list[str] = []
    for key in REQUIRED_DEPLOYMENT_KEYS:
        if not _first_non_empty(merged_deployment.get(key)):
            missing.append(f"deployment.{key}")
    path = _first_non_empty(merged_execution.get("path"), "/")
    if path is None:
        missing.append("execution.path")
    base_url = _first_non_empty(
        merged_execution.get("baseUrl"),
        connector.get("baseUrl"),
        connector_execution.get("baseUrl"),
    )
    if not base_url:
        missing.append("connector.baseUrl")
    if missing:
        raise SystemExit(
            "Missing required REST Source configuration: "
            + ", ".join(missing)
            + ". Bind a REST connector (baseUrl) and set region + path on the step."
        )

    method = str(_first_non_empty(merged_execution.get("method"), "GET") or "GET").upper()
    timeout_ms = _first_non_empty(
        merged_execution.get("timeoutMs"),
        connector.get("timeoutMs"),
        connector_execution.get("timeoutMs"),
        30000,
    )
    timeout_sec = float(timeout_ms) / 1000.0
    if _first_non_empty(merged_execution.get("timeoutSec")) is not None:
        timeout_sec = float(merged_execution["timeoutSec"])

    headers = {
        **_as_str_map(connector.get("headers")),
        **_as_str_map(connector_execution.get("headers")),
        **_as_str_map(merged_execution.get("headers")),
    }
    api_key = _first_non_empty(
        service_execution.get("api_key"),
        service_execution.get("apiKey"),
        merged_execution.get("api_key"),
        merged_execution.get("apiKey"),
        connector.get("api_key"),
        connector_execution.get("api_key"),
    )
    if api_key and "Authorization" not in headers and "X-API-Key" not in headers:
        headers["X-API-Key"] = str(api_key)
    bearer = _first_non_empty(
        service_execution.get("bearerToken"),
        merged_execution.get("bearerToken"),
        connector.get("bearerToken"),
    )
    if bearer and "Authorization" not in headers:
        headers["Authorization"] = f"Bearer {bearer}"

    query = {
        **_as_str_map(connector_execution.get("query")),
        **_as_str_map(merged_execution.get("query")),
    }
    body = merged_execution.get("body")
    if method in ("GET", "HEAD", "DELETE"):
        body = None

    merged_execution = {
        **merged_execution,
        "path": str(path),
        "method": method,
        "baseUrl": str(base_url),
        "timeoutMs": str(int(timeout_sec * 1000)),
    }
    merged_deployment = {
        **merged_deployment,
        "region": str(_first_non_empty(merged_deployment.get("region"))),
    }

    return ResolvedRestSourceConfig(
        base_url=str(base_url),
        path=str(path),
        method=method,
        timeout_sec=timeout_sec,
        headers=headers,
        query=query,
        body=body,
        deployment=merged_deployment,
        execution=merged_execution,
        connector=connector_execution if connector_execution else connector,
    )


def resolve_from_env() -> ResolvedRestSourceConfig:
    defaults = load_json_env("PIPELET_DEFAULTS", {})
    return resolve_config(
        connector=load_json_env("CONNECTOR_CONFIG"),
        service=load_json_env("SERVICE_CONFIG"),
        deployment=load_json_env("DEPLOYMENT_CONFIG"),
        execution=load_json_env("EXECUTION_CONFIG"),
        pipelet_deployment_defaults=defaults.get("deploymentConfiguration")
        or defaults.get("deployment_config"),
        pipelet_execution_defaults=defaults.get("executionConfiguration")
        or defaults.get("execution_config"),
    )
