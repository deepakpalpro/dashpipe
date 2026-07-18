"""Generic CSV → JSON records pipelet."""

from __future__ import annotations

import csv
import io
from typing import Any


def parse_csv_text(
    text: str,
    *,
    delimiter: str = ",",
    has_header: bool = True,
) -> list[dict[str, Any]]:
    if not text or not text.strip():
        return []
    stream = io.StringIO(text)
    if has_header:
        reader = csv.DictReader(stream, delimiter=delimiter)
        rows: list[dict[str, Any]] = []
        for row in reader:
            cleaned = {
                (k or "").strip(): (v or "").strip()
                for k, v in row.items()
                if k is not None
            }
            if any(cleaned.values()):
                rows.append(cleaned)
        return rows

    reader_list = csv.reader(stream, delimiter=delimiter)
    rows = []
    for idx, cols in enumerate(reader_list):
        if not any(c.strip() for c in cols):
            continue
        rows.append({f"col{i}": c.strip() for i, c in enumerate(cols)})
    return rows


def _looks_like_csv(text: str) -> bool:
    """Reject orchestrator kickoffs like payload=\"run-<executionId>\"."""
    stripped = text.strip()
    if not stripped or stripped.startswith("run-"):
        return False
    return ("\n" in stripped) or ("," in stripped)


def extract_csv_text(message: dict[str, Any]) -> str:
    # Prefer explicit S3 / CSV fields before generic "payload" (stage kickoffs use payload=run-…).
    for key in ("csv", "content", "text"):
        value = message.get(key)
        if isinstance(value, str) and value.strip() and _looks_like_csv(value):
            return value
    payload = message.get("payload")
    if isinstance(payload, str) and payload.strip() and _looks_like_csv(payload):
        return payload
    records = message.get("records")
    if isinstance(records, list) and records and isinstance(records[0], str):
        joined = "\n".join(records)
        if _looks_like_csv(joined):
            return joined
    return ""


def run(message: dict[str, Any], execution: dict[str, Any]) -> dict[str, Any]:
    delimiter = str(execution.get("delimiter") or ",")
    has_header_raw = str(execution.get("hasHeader", "true")).strip().lower()
    has_header = has_header_raw in ("1", "true", "yes", "y")
    csv_text = extract_csv_text(message)
    if not csv_text:
        keys = sorted(message.keys()) if isinstance(message, dict) else []
        raise SystemExit(
            "No CSV text found on message (expected content/csv). "
            f"keys={keys}. Stale kickoff payloads (payload=run-…) are ignored."
        )
    records = parse_csv_text(csv_text, delimiter=delimiter, has_header=has_header)
    if not records:
        raise SystemExit(
            "CSV parsed to 0 records — check delimiter/hasHeader and source content"
        )
    return {
        **message,
        "records": records,
        "recordCount": len(records),
        "pipeletId": "plet-csv-to-json",
        "execution": {
            "delimiter": delimiter,
            "hasHeader": str(has_header).lower(),
        },
    }
