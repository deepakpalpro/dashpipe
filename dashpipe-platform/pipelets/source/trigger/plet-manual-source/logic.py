"""Manual source: emit an ad-hoc payload from UI/API run, or a default kickoff envelope."""
from __future__ import annotations

from typing import Any


def _records(message: dict[str, Any]) -> list[Any]:
    rec = message.get("records")
    if isinstance(rec, list):
        return rec
    payload = message.get("payload")
    if isinstance(payload, list):
        return payload
    if payload is not None:
        return [payload] if not isinstance(payload, dict) else [payload]
    return []


def run(
    message: dict[str, Any],
    execution: dict[str, Any],
    connector: dict[str, Any],
    pipelet_id: str,
) -> dict[str, Any]:
    out = dict(message)
    out["pipeletId"] = pipelet_id
    out["execution"] = {
        k: v
        for k, v in execution.items()
        if "password" not in k.lower() and "secret" not in k.lower()
    }

    # Prefer an ad-hoc body supplied at trigger time (TRIGGER_PAYLOAD / run API).
    if message.get("payload") is not None or message.get("records"):
        payload = message.get("payload")
        if payload is None:
            payload = message.get("records")
        records = _records({"payload": payload, "records": message.get("records")})
        if not records and payload is not None:
            records = [payload] if not isinstance(payload, list) else payload
        out["payload"] = payload
        out["records"] = records
        out["recordCount"] = len(records)
        return out

    # Default kickoff when Run is clicked with no custom JSON body.
    out["payload"] = {"trigger": pipelet_id, "execution": execution}
    out["records"] = [out["payload"]]
    out["recordCount"] = 1
    return out
