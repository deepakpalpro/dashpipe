"""Shared config merge helpers for generic Python pipelets."""

from __future__ import annotations

import json
import os
import sys
from typing import Any


def log(msg: str) -> None:
    print(msg, file=sys.stderr, flush=True)


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


def first_non_empty(*values: Any) -> Any:
    for value in values:
        if value is None:
            continue
        if isinstance(value, str) and value.strip() == "":
            continue
        return value
    return None


def require_keys(label: str, data: dict[str, Any], keys: tuple[str, ...]) -> list[str]:
    missing: list[str] = []
    for key in keys:
        if not first_non_empty(data.get(key)):
            missing.append(f"{label}.{key}")
    return missing


def connector_maps(connector: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
    deployment = connector.get("deployment_config") or connector.get(
        "deploymentConfiguration"
    )
    execution = (
        connector.get("execution_config")
        or connector.get("executionConfiguration")
        or connector.get("config")
    )
    if not isinstance(deployment, dict):
        deployment = {}
    if not isinstance(execution, dict):
        execution = {
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
    return deployment, execution


def service_map(service: dict[str, Any]) -> dict[str, Any]:
    mapped = (
        service.get("execution_config")
        or service.get("tenant_config")
        or service.get("executionConfiguration")
        or service
    )
    return mapped if isinstance(mapped, dict) else {}


def resolve_layers(
    *,
    required_deployment: tuple[str, ...] = (),
    required_execution: tuple[str, ...] = (),
    pipelet_deployment_defaults: dict[str, Any] | None = None,
    pipelet_execution_defaults: dict[str, Any] | None = None,
    connector: dict[str, Any] | None = None,
    service: dict[str, Any] | None = None,
    deployment: dict[str, Any] | None = None,
    execution: dict[str, Any] | None = None,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    connector = connector or {}
    service = service or {}
    c_dep, c_exec = connector_maps(connector)
    s_exec = service_map(service)

    merged_deployment = shallow_merge(
        pipelet_deployment_defaults, c_dep, deployment
    )
    merged_execution = shallow_merge(
        pipelet_execution_defaults, c_exec, s_exec, execution
    )

    missing = require_keys("deployment", merged_deployment, required_deployment)
    missing += require_keys("execution", merged_execution, required_execution)
    if missing:
        raise SystemExit(
            "Missing required configuration: "
            + ", ".join(missing)
            + ". Set Keys on the pipeline step and/or bind connector/service."
        )
    return merged_deployment, merged_execution, {**connector, **s_exec}


def resolve_from_env(
    *,
    required_deployment: tuple[str, ...] = (),
    required_execution: tuple[str, ...] = (),
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    defaults = load_json_env("PIPELET_DEFAULTS", {})
    return resolve_layers(
        required_deployment=required_deployment,
        required_execution=required_execution,
        pipelet_deployment_defaults=defaults.get("deploymentConfiguration")
        or defaults.get("deployment_config"),
        pipelet_execution_defaults=defaults.get("executionConfiguration")
        or defaults.get("execution_config"),
        connector=load_json_env("CONNECTOR_CONFIG"),
        service=load_json_env("SERVICE_CONFIG"),
        deployment=load_json_env("DEPLOYMENT_CONFIG"),
        execution=load_json_env("EXECUTION_CONFIG"),
    )


def read_stdin_json() -> dict[str, Any]:
    raw = sys.stdin.read()
    if not raw.strip():
        return {}
    value = json.loads(raw, strict=False)
    if not isinstance(value, dict):
        raise SystemExit("stdin must be a JSON object")
    return value


def write_stdout_json(payload: dict[str, Any]) -> None:
    print(json.dumps(payload), flush=True)
