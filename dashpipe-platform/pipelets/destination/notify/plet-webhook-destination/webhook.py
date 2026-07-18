"""Generic HTTP webhook / REST destination."""

from __future__ import annotations

import json
import urllib.error
import urllib.request
from typing import Any

from config_merge import first_non_empty


def resolve_url(connector: dict[str, Any], execution: dict[str, Any]) -> str:
    base = first_non_empty(
        execution.get("baseUrl"),
        connector.get("baseUrl"),
        connector.get("url"),
    )
    path = first_non_empty(execution.get("path"), "/")
    if base is None:
        raise SystemExit(
            "Missing connector.baseUrl (or execution.baseUrl). Bind a REST connector."
        )
    base_s = str(base).rstrip("/")
    path_s = str(path)
    if not path_s.startswith("/"):
        path_s = "/" + path_s
    return base_s + path_s


def _is_kickoff_payload(value: Any) -> bool:
    return isinstance(value, str) and value.strip().startswith("run-")


def _as_upload_body(value: Any) -> Any:
    """Normalize to a Petstore-friendly {mode, items} object when possible."""
    if isinstance(value, list):
        return {"mode": "upsert", "items": value}
    if isinstance(value, dict):
        if isinstance(value.get("items"), list):
            return value
        if isinstance(value.get("records"), list):
            return {
                "mode": str(value.get("mode") or "upsert"),
                "items": value["records"],
            }
    return value


def extract_body(message: dict[str, Any], execution: dict[str, Any]) -> Any:
    body_key = str(execution.get("bodyKey") or "").strip()
    if body_key and body_key in message:
        return _as_upload_body(message[body_key])

    for key in ("petstorePayload", "body"):
        if key in message and message[key] is not None and not _is_kickoff_payload(message[key]):
            return _as_upload_body(message[key])

    payload = message.get("payload")
    if payload is not None and not _is_kickoff_payload(payload):
        # Prefer structured wrap from field-mapper; skip orchestrator kickoff strings.
        if isinstance(payload, (dict, list)):
            return _as_upload_body(payload)

    if isinstance(message.get("records"), list):
        return {
            "mode": str(execution.get("mode") or "upsert"),
            "items": message["records"],
        }

    if isinstance(message.get("items"), list):
        return {
            "mode": str(execution.get("mode") or message.get("mode") or "upsert"),
            "items": message["items"],
        }

    raise SystemExit(
        "Webhook body missing: expected petstorePayload/payload.items/records "
        f"(message keys={sorted(message.keys())})"
    )


def resolve_timeout(execution: dict[str, Any]) -> float:
    if first_non_empty(execution.get("timeoutSec")) is not None:
        return float(execution["timeoutSec"])
    if first_non_empty(execution.get("timeoutMs")) is not None:
        return float(execution["timeoutMs"]) / 1000.0
    return 30.0


def post_json(
    url: str, body: Any, *, method: str = "POST", timeout: float = 30.0
) -> dict[str, Any]:
    if isinstance(body, dict) and isinstance(body.get("items"), list) and not body["items"]:
        raise SystemExit("Refusing to POST empty items array to destination")
    data = json.dumps(body, default=str).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method=method.upper(),
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw.strip() else {"status": resp.status}
    except urllib.error.HTTPError as exc:
        err = exc.read().decode("utf-8", errors="replace")
        raise SystemExit(f"HTTP {exc.code} from {url}: {err}") from exc


def run(
    message: dict[str, Any],
    execution: dict[str, Any],
    connector: dict[str, Any],
) -> dict[str, Any]:
    url = resolve_url(connector, execution)
    method = str(execution.get("method") or "POST").upper()
    timeout = resolve_timeout(execution)
    body = extract_body(message, execution)
    result = post_json(url, body, method=method, timeout=timeout)
    return {
        **message,
        "pipeletId": "plet-webhook-destination",
        "uploadResult": result,
        "http": {"url": url, "method": method},
        "execution": {
            "path": execution.get("path"),
            "method": method,
            "baseUrl": execution.get("baseUrl") or connector.get("baseUrl"),
        },
    }
