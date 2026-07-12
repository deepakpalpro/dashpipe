"""
plet-csv-source config merge.

Layers (later wins):
  pipelet defaults → CONNECTOR_CONFIG → SERVICE_CONFIG → DEPLOYMENT_CONFIG / EXECUTION_CONFIG

Required after merge:
  deployment.region
  execution.objectKey
  connector.bucket
"""

from __future__ import annotations

import json
import os
from typing import Any

REQUIRED_DEPLOYMENT_KEYS = ("region",)
REQUIRED_EXECUTION_KEYS = ("objectKey",)


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


def resolve_object_key(execution: dict[str, Any]) -> str | None:
    key = _first_non_empty(
        execution.get("objectKey"),
        execution.get("key"),
        execution.get("path"),
        execution.get("filePath"),
    )
    return str(key).strip() if key is not None else None


def resolve_credentials(
    connector: dict[str, Any], service: dict[str, Any]
) -> tuple[str, str]:
    access = _first_non_empty(
        service.get("accessKeyId"),
        service.get("access_key"),
        service.get("accessKey"),
        connector.get("accessKeyId"),
        connector.get("access_key"),
        os.environ.get("AWS_ACCESS_KEY_ID"),
        "test",
    )
    secret = _first_non_empty(
        service.get("secretAccessKey"),
        service.get("secret_key"),
        service.get("secretAccessKey"),
        connector.get("secretAccessKey"),
        connector.get("secret_key"),
        os.environ.get("AWS_SECRET_ACCESS_KEY"),
        "test",
    )
    return str(access), str(secret)


class ResolvedCsvSourceConfig:
    def __init__(
        self,
        *,
        bucket: str,
        object_key: str,
        region: str,
        endpoint: str | None,
        access_key_id: str,
        secret_access_key: str,
        delimiter: str,
        has_header: bool,
        deployment: dict[str, Any],
        execution: dict[str, Any],
    ) -> None:
        self.bucket = bucket
        self.object_key = object_key
        self.region = region
        self.endpoint = endpoint
        self.access_key_id = access_key_id
        self.secret_access_key = secret_access_key
        self.delimiter = delimiter
        self.has_header = has_header
        self.deployment = deployment
        self.execution = execution


def resolve_config(
    *,
    connector: dict[str, Any] | None = None,
    service: dict[str, Any] | None = None,
    deployment: dict[str, Any] | None = None,
    execution: dict[str, Any] | None = None,
    pipelet_deployment_defaults: dict[str, Any] | None = None,
    pipelet_execution_defaults: dict[str, Any] | None = None,
) -> ResolvedCsvSourceConfig:
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
        {k: connector.get(k) for k in ("region", "cloud", "endpoint") if k in connector},
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
    object_key = resolve_object_key(merged_execution)
    if not object_key:
        missing.append("execution.objectKey")
    bucket = _first_non_empty(
        merged_execution.get("bucket"),
        connector.get("bucket"),
        merged_deployment.get("bucket"),
    )
    if not bucket:
        missing.append("connector.bucket")
    if missing:
        raise SystemExit(
            "Missing required CSV Source configuration: "
            + ", ".join(missing)
            + ". Bind a storage connector (bucket) and set region + objectKey on the step."
        )

    region = str(_first_non_empty(merged_deployment.get("region")))
    endpoint = _first_non_empty(
        merged_deployment.get("endpoint"),
        merged_execution.get("endpoint"),
        connector.get("endpoint"),
    )
    access_key, secret_key = resolve_credentials(connector, service_execution)

    delimiter = str(_first_non_empty(merged_execution.get("delimiter"), ",") or ",")
    has_header_raw = str(
        _first_non_empty(merged_execution.get("hasHeader"), "true")
    ).strip().lower()
    has_header = has_header_raw in ("1", "true", "yes", "y")

    merged_execution = {
        **merged_execution,
        "objectKey": object_key,
        "bucket": str(bucket),
        "delimiter": delimiter,
        "hasHeader": str(has_header).lower(),
    }
    merged_deployment = {**merged_deployment, "region": region}
    if endpoint:
        merged_deployment["endpoint"] = str(endpoint)

    return ResolvedCsvSourceConfig(
        bucket=str(bucket),
        object_key=str(object_key),
        region=region,
        endpoint=str(endpoint) if endpoint else None,
        access_key_id=access_key,
        secret_access_key=secret_key,
        delimiter=delimiter,
        has_header=has_header,
        deployment=merged_deployment,
        execution=merged_execution,
    )


def resolve_from_env() -> ResolvedCsvSourceConfig:
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
