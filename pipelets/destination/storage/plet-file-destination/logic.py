"""Write pipeline payloads to a local / mounted filesystem path."""
from __future__ import annotations

import json
from pathlib import Path
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


def _resolve_base(execution: dict[str, Any], connector: dict[str, Any]) -> Path:
    raw = str(
        execution.get("basePath")
        or execution.get("path")
        or connector.get("basePath")
        or connector.get("mountPath")
        or connector.get("path")
        or ""
    ).strip()
    if not raw:
        raise SystemExit(
            "execution.basePath (or CONNECTOR_CONFIG.basePath/mountPath) is required "
            "for mounted-path writes"
        )
    return Path(raw).expanduser().resolve()


def _resolve_relative_key(execution: dict[str, Any]) -> str:
    rel = str(
        execution.get("objectKey")
        or execution.get("fileName")
        or execution.get("key")
        or ""
    ).strip()
    if not rel:
        raise SystemExit("execution.objectKey (relative path under basePath) is required")
    path = Path(rel)
    if path.is_absolute() or ".." in path.parts:
        raise SystemExit("execution.objectKey must be a relative path without '..'")
    return rel


def _body_text(message: dict[str, Any], execution: dict[str, Any]) -> tuple[str, str]:
    body_obj: Any = message.get("payload")
    if body_obj is None:
        body_obj = message.get("records") or message
    fmt = str(execution.get("format") or "json").strip().lower()
    if fmt in ("ndjson", "jsonl"):
        records = _records(message) or ([body_obj] if body_obj is not None else [])
        return "".join(json.dumps(r, default=str) + "\n" for r in records), fmt
    return json.dumps(body_obj, default=str, indent=2) + "\n", "json"


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

    base_path = _resolve_base(execution, connector)
    rel = _resolve_relative_key(execution)
    target = (base_path / rel).resolve()
    try:
        target.relative_to(base_path)
    except ValueError as exc:
        raise SystemExit("execution.objectKey escapes basePath") from exc

    text, fmt = _body_text(message, execution)
    target.parent.mkdir(parents=True, exist_ok=True)
    append = str(execution.get("append") or "").strip().lower() in ("1", "true", "yes")
    with target.open("a" if append else "w", encoding="utf-8") as fh:
        fh.write(text)

    out.update(
        {
            "written": True,
            "path": str(target),
            "basePath": str(base_path),
            "objectKey": rel,
            "bytes": len(text.encode("utf-8")),
            "recordCount": len(_records(message)),
            "append": append,
            "format": fmt,
        }
    )
    return out
