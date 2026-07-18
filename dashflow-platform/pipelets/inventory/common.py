"""Shared helpers for inventory pipelets (S3, CSV, filter, map, REST)."""

from __future__ import annotations

import csv
import io
import json
import os
import re
import sys
import urllib.error
import urllib.request
from typing import Any

import boto3
from botocore.client import Config

ALLOWED_CATEGORIES = frozenset({"food", "accessories", "toys"})
SKU_PATTERN = re.compile(r"^[A-Z]{2,10}-\d{2,10}$")


def env(name: str, default: str | None = None) -> str:
    value = os.environ.get(name, default)
    if value is None or value == "":
        raise SystemExit(f"Missing required env var: {name}")
    return value


def env_optional(name: str, default: str = "") -> str:
    return os.environ.get(name, default) or default


def log(msg: str) -> None:
    print(msg, file=sys.stderr, flush=True)


def load_json_env(name: str, default: dict[str, Any] | None = None) -> dict[str, Any]:
    raw = os.environ.get(name)
    if not raw:
        return default or {}
    return json.loads(raw)


def s3_client():
    cfg = load_json_env("CONNECTOR_CONFIG", {})
    endpoint = cfg.get("endpoint") or env_optional(
        "S3_ENDPOINT", "http://host.docker.internal:4567"
    )
    region = cfg.get("region") or env_optional("S3_REGION", "us-east-1")
    access_key = cfg.get("accessKeyId") or env_optional("AWS_ACCESS_KEY_ID", "test")
    secret_key = cfg.get("secretAccessKey") or env_optional(
        "AWS_SECRET_ACCESS_KEY", "test"
    )
    return boto3.client(
        "s3",
        endpoint_url=endpoint,
        region_name=region,
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        config=Config(s3={"addressing_style": "path"}),
    )


def fetch_s3_object(bucket: str, key: str) -> bytes:
    client = s3_client()
    log(f"S3 get s3://{bucket}/{key}")
    obj = client.get_object(Bucket=bucket, Key=key)
    return obj["Body"].read()


def parse_csv(content: bytes | str) -> list[dict[str, str]]:
    text = content.decode("utf-8-sig") if isinstance(content, (bytes, bytearray)) else content
    reader = csv.DictReader(io.StringIO(text))
    rows: list[dict[str, str]] = []
    for row in reader:
        cleaned = { (k or "").strip(): (v or "").strip() for k, v in row.items() if k }
        if any(cleaned.values()):
            rows.append(cleaned)
    log(f"Parsed {len(rows)} CSV rows")
    return rows


def normalize_row(row: dict[str, str]) -> dict[str, Any]:
    sku = row.get("sku", "").strip()
    name = row.get("name", "").strip()
    category = row.get("category", "").strip().lower()
    qty_raw = row.get("quantity", "0").strip()
    price_raw = (
        row.get("unitPrice")
        or row.get("unit_price")
        or "0"
    ).strip()
    description = row.get("description", "").strip() or None
    try:
        quantity = int(float(qty_raw))
    except ValueError:
        quantity = -1
    try:
        unit_price = float(price_raw)
    except ValueError:
        unit_price = -1.0
    return {
        "sku": sku,
        "name": name,
        "category": category,
        "quantity": quantity,
        "unitPrice": unit_price,
        "description": description,
    }


def filter_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    kept: list[dict[str, Any]] = []
    for row in rows:
        if row["category"] not in ALLOWED_CATEGORIES:
            continue
        if row["quantity"] <= 0:
            continue
        if not SKU_PATTERN.match(row["sku"]):
            continue
        if not row["name"]:
            continue
        if row["unitPrice"] < 0:
            continue
        kept.append(row)
    log(f"Filter kept {len(kept)} of {len(rows)} rows")
    return kept


def map_petstore_payload(rows: list[dict[str, Any]], mode: str = "upsert") -> dict[str, Any]:
    items = []
    for row in rows:
        item = {
            "sku": row["sku"],
            "name": row["name"],
            "category": row["category"],
            "quantity": row["quantity"],
            "unitPrice": row["unitPrice"],
        }
        if row.get("description"):
            item["description"] = row["description"]
        items.append(item)
    payload = {"mode": mode, "items": items}
    log(f"Mapped payload mode={mode} items={len(items)}")
    return payload


def post_json(url: str, body: dict[str, Any], timeout: float = 30.0) -> dict[str, Any]:
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )
    log(f"POST {url} ({len(body.get('items', []))} items)")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        err_body = exc.read().decode("utf-8", errors="replace")
        raise SystemExit(f"HTTP {exc.code} from {url}: {err_body}") from exc


def resolve_upload_url() -> str:
    cfg = load_json_env("CONNECTOR_CONFIG", {})
    exec_cfg = load_json_env("EXECUTION_CONFIG", {})
    base = (
        exec_cfg.get("baseUrl")
        or cfg.get("baseUrl")
        or env_optional("PETSTORE_BASE_URL", "http://host.docker.internal:4010/api/v3")
    ).rstrip("/")
    path = exec_cfg.get("path") or "/inventory/upload"
    if not path.startswith("/"):
        path = "/" + path
    return base + path


def run_batch() -> dict[str, Any]:
    """Full inventory pipeline in one process (S3 → parse → filter → map → POST)."""
    s3_cfg = load_json_env("S3_CONNECTOR_CONFIG") or load_json_env("CONNECTOR_CONFIG", {})
    # Prefer dedicated S3 config when batch uses multiple connectors
    if s3_cfg:
        os.environ["CONNECTOR_CONFIG"] = json.dumps(s3_cfg)

    exec_cfg = load_json_env("EXECUTION_CONFIG", {})
    bucket = (
        exec_cfg.get("bucket")
        or s3_cfg.get("bucket")
        or env_optional("S3_BUCKET", "demo-s3-source")
    )
    key = (
        exec_cfg.get("objectKey")
        or exec_cfg.get("key")
        or env_optional("S3_OBJECT_KEY", "inventory/daily.csv")
    )
    mode = exec_cfg.get("mode") or "upsert"

    content = fetch_s3_object(bucket, key)
    raw_rows = parse_csv(content)
    normalized = [normalize_row(r) for r in raw_rows]
    filtered = filter_rows(normalized)
    payload = map_petstore_payload(filtered, mode=mode)

    # Switch connector context to REST for upload URL resolution
    rest_cfg = load_json_env("REST_CONNECTOR_CONFIG", {})
    if rest_cfg:
        os.environ["CONNECTOR_CONFIG"] = json.dumps(rest_cfg)

    url = resolve_upload_url()
    result = post_json(url, payload)
    log(f"Upload result: {json.dumps(result)}")
    return {"payload": payload, "result": result}


def stage_dispatch(pipelet_id: str) -> Any:
    """Run a single catalog pipelet stage (stdin JSON → stdout JSON)."""
    raw = sys.stdin.read()
    message = json.loads(raw) if raw.strip() else {}

    if pipelet_id == "plet-s3-source":
        exec_cfg = load_json_env("EXECUTION_CONFIG", {})
        cfg = load_json_env("CONNECTOR_CONFIG", {})
        bucket = exec_cfg.get("bucket") or cfg.get("bucket") or "demo-s3-source"
        key = exec_cfg.get("objectKey") or exec_cfg.get("key") or "inventory/daily.csv"
        content = fetch_s3_object(bucket, key)
        out = {
            **message,
            "contentType": "text/csv",
            "csv": content.decode("utf-8-sig"),
            "bucket": bucket,
            "key": key,
        }
        print(json.dumps(out))
        return out

    if pipelet_id in ("plet-csv-to-json", "plet-csv-source"):
        csv_text = message.get("csv") or message.get("payload") or ""
        rows = [normalize_row(r) for r in parse_csv(csv_text)]
        out = {**message, "records": rows}
        print(json.dumps(out))
        return out

    if pipelet_id in ("plet-python-filter", "plet-sample-filter"):
        records = message.get("records") or []
        out = {**message, "records": filter_rows(records)}
        print(json.dumps(out))
        return out

    if pipelet_id in ("plet-field-mapper", "plet-json-transform"):
        records = message.get("records") or []
        mode = load_json_env("EXECUTION_CONFIG", {}).get("mode", "upsert")
        payload = map_petstore_payload(records, mode=mode)
        out = {**message, "petstorePayload": payload}
        print(json.dumps(out))
        return out

    if pipelet_id == "plet-webhook-destination":
        payload = message.get("petstorePayload")
        if not payload:
            raise SystemExit("Missing petstorePayload for webhook destination")
        result = post_json(resolve_upload_url(), payload)
        out = {**message, "uploadResult": result}
        print(json.dumps(out))
        return out

    raise SystemExit(f"Unsupported pipelet id: {pipelet_id}")
