"""Fetch CSV from object storage and parse into records."""

from __future__ import annotations

import csv
import io
from typing import Any

from config import ResolvedCsvSourceConfig


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

    rows = []
    for cols in csv.reader(stream, delimiter=delimiter):
        if not any(c.strip() for c in cols):
            continue
        rows.append({f"col{i}": c.strip() for i, c in enumerate(cols)})
    return rows


def build_s3_client(cfg: ResolvedCsvSourceConfig):
    import boto3
    from botocore.client import Config

    kwargs: dict[str, Any] = {
        "region_name": cfg.region,
        "aws_access_key_id": cfg.access_key_id,
        "aws_secret_access_key": cfg.secret_access_key,
        "config": Config(s3={"addressing_style": "path"}),
    }
    if cfg.endpoint:
        kwargs["endpoint_url"] = cfg.endpoint
    return boto3.client("s3", **kwargs)


def fetch_and_parse(cfg: ResolvedCsvSourceConfig) -> dict[str, Any]:
    client = build_s3_client(cfg)
    response = client.get_object(Bucket=cfg.bucket, Key=cfg.object_key)
    body: bytes = response["Body"].read()
    text = body.decode("utf-8-sig")
    records = parse_csv_text(
        text, delimiter=cfg.delimiter, has_header=cfg.has_header
    )
    if not records:
        raise SystemExit(
            "CSV Source parsed 0 records — check delimiter/hasHeader and object content "
            f"(bucket={cfg.bucket} key={cfg.object_key} bytes={len(body)})"
        )
    return {
        "pipeletId": "plet-csv-source",
        "bucket": cfg.bucket,
        "key": cfg.object_key,
        "objectKey": cfg.object_key,
        "region": cfg.region,
        "contentType": response.get("ContentType") or "text/csv",
        "size": len(body),
        "content": text,
        "contentEncoding": "utf-8",
        "records": records,
        "recordCount": len(records),
        "deployment": cfg.deployment,
        "execution": {
            k: v
            for k, v in cfg.execution.items()
            if k not in ("secretAccessKey", "secret_key")
        },
    }
