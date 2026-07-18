"""S3 client + object fetch for plet-s3-source."""

from __future__ import annotations

import base64
import mimetypes
from typing import Any

import boto3
from botocore.client import Config

from config import ResolvedS3SourceConfig


def build_s3_client(cfg: ResolvedS3SourceConfig):
    kwargs: dict[str, Any] = {
        "region_name": cfg.region,
        "aws_access_key_id": cfg.access_key_id,
        "aws_secret_access_key": cfg.secret_access_key,
        "config": Config(s3={"addressing_style": "path"}),
    }
    if cfg.endpoint:
        kwargs["endpoint_url"] = cfg.endpoint
    return boto3.client("s3", **kwargs)


def fetch_object(cfg: ResolvedS3SourceConfig) -> dict[str, Any]:
    client = build_s3_client(cfg)
    response = client.get_object(Bucket=cfg.bucket, Key=cfg.object_key)
    body: bytes = response["Body"].read()
    content_type = response.get("ContentType") or mimetypes.guess_type(cfg.object_key)[0]
    text_like = False
    if content_type and (
        content_type.startswith("text/")
        or content_type in ("application/json", "application/csv", "application/xml")
    ):
        text_like = True
    elif cfg.object_key.lower().endswith((".csv", ".json", ".txt", ".log", ".xml")):
        text_like = True

    record: dict[str, Any] = {
        "pipeletId": "plet-s3-source",
        "bucket": cfg.bucket,
        "key": cfg.object_key,
        "objectKey": cfg.object_key,
        "region": cfg.region,
        "contentType": content_type or "application/octet-stream",
        "size": len(body),
        "etag": (response.get("ETag") or "").strip('"') or None,
        "deployment": cfg.deployment,
        "execution": {
            k: v
            for k, v in cfg.execution.items()
            if k not in ("secretAccessKey", "secret_key")
        },
    }
    if text_like:
        record["contentEncoding"] = "utf-8"
        record["content"] = body.decode("utf-8-sig")
    else:
        record["contentEncoding"] = "base64"
        record["content"] = base64.b64encode(body).decode("ascii")
    return record
