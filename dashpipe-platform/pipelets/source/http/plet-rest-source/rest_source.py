"""HTTP fetch for plet-rest-source."""

from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request
from typing import Any

from config import ResolvedRestSourceConfig


def _build_url(cfg: ResolvedRestSourceConfig) -> str:
    url = cfg.url
    if cfg.query:
        sep = "&" if "?" in url else "?"
        url = url + sep + urllib.parse.urlencode(cfg.query)
    return url


def _normalize_payload(parsed: Any) -> tuple[Any, list[dict[str, Any]] | None, int]:
    """Return (payload, records_or_none, record_count)."""
    if isinstance(parsed, list):
        records = [r if isinstance(r, dict) else {"value": r} for r in parsed]
        return parsed, records, len(records)
    if isinstance(parsed, dict):
        for key in ("records", "items", "data", "results"):
            value = parsed.get(key)
            if isinstance(value, list):
                records = [r if isinstance(r, dict) else {"value": r} for r in value]
                return parsed, records, len(records)
        return parsed, None, 1
    return parsed, [{"value": parsed}], 1


def fetch(cfg: ResolvedRestSourceConfig) -> dict[str, Any]:
    url = _build_url(cfg)
    headers = {
        "Accept": "application/json",
        **cfg.headers,
    }
    data = None
    if cfg.body is not None:
        data = json.dumps(cfg.body, default=str).encode("utf-8")
        headers.setdefault("Content-Type", "application/json")

    req = urllib.request.Request(url, data=data, method=cfg.method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=cfg.timeout_sec) as resp:
            status = getattr(resp, "status", 200) or 200
            raw = resp.read().decode("utf-8")
            content_type = resp.headers.get("Content-Type") or "application/json"
    except urllib.error.HTTPError as exc:
        err = exc.read().decode("utf-8", errors="replace")
        raise SystemExit(f"HTTP {exc.code} from {url}: {err}") from exc
    except urllib.error.URLError as exc:
        raise SystemExit(f"Request failed for {url}: {exc}") from exc

    parsed: Any
    if raw.strip():
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            parsed = {"content": raw}
    else:
        parsed = {}

    payload, records, record_count = _normalize_payload(parsed)
    out: dict[str, Any] = {
        "pipeletId": "plet-rest-source",
        "payload": payload,
        "recordCount": record_count,
        "http": {
            "url": url,
            "method": cfg.method,
            "status": status,
            "contentType": content_type,
        },
        "deployment": cfg.deployment,
        "execution": {
            k: v
            for k, v in cfg.execution.items()
            if k not in ("api_key", "apiKey", "bearerToken", "headers")
        },
    }
    if records is not None:
        out["records"] = records
    return out
