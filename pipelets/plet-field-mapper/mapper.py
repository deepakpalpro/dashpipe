"""Generic field mapper — rename/project fields; optional wrap payload."""

from __future__ import annotations

import json
from typing import Any


def parse_mapping(raw: Any) -> dict[str, str]:
    """
    Accept:
      - JSON object string: {"unit_price":"unitPrice","sku":"sku"}
      - CSV pairs: unit_price=unitPrice,sku=sku
      - already a dict
    """
    if raw is None or raw == "":
        return {}
    if isinstance(raw, dict):
        return {str(k): str(v) for k, v in raw.items()}
    text = str(raw).strip()
    if not text:
        return {}
    if text.startswith("{"):
        data = json.loads(text)
        if not isinstance(data, dict):
            raise SystemExit("execution.mapping JSON must be an object")
        return {str(k): str(v) for k, v in data.items()}
    mapping: dict[str, str] = {}
    for part in text.split(","):
        part = part.strip()
        if not part:
            continue
        if "=" not in part:
            raise SystemExit(f"Invalid mapping pair (expected from=to): {part}")
        src, dst = part.split("=", 1)
        mapping[src.strip()] = dst.strip()
    return mapping


def map_record(record: dict[str, Any], mapping: dict[str, str]) -> dict[str, Any]:
    if not mapping:
        return dict(record)
    out: dict[str, Any] = {}
    for src, dst in mapping.items():
        if src in record:
            out[dst] = record[src]
    return out


def run(message: dict[str, Any], execution: dict[str, Any]) -> dict[str, Any]:
    mode = str(execution.get("mode") or "map").strip()
    mapping = parse_mapping(execution.get("mapping"))
    records = message.get("records") or []
    if not isinstance(records, list):
        raise SystemExit("message.records must be a list")

    mapped = [
        map_record(r, mapping) if isinstance(r, dict) else r for r in records
    ]

    out: dict[str, Any] = {
        **message,
        "records": mapped,
        "recordCount": len(mapped),
        "pipeletId": "plet-field-mapper",
        "execution": {"mode": mode, "mapping": mapping},
    }

    # Optional wrap modes for bulk sinks
    if mode in ("upsert", "replace", "wrap"):
        items_key = str(execution.get("itemsKey") or "items")
        payload = {"mode": mode if mode != "wrap" else "upsert", items_key: mapped}
        wrap_key = str(execution.get("wrapKey") or "payload")
        out[wrap_key] = payload
        # Convenience alias used by inventory/webhook demos
        if wrap_key != "petstorePayload":
            out.setdefault("petstorePayload", payload)

    return out
