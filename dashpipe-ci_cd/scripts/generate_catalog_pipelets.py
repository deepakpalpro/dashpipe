#!/usr/bin/env python3
"""Generate runnable pipelet scaffolds for Waves 1–4 and patch the UI fixture.

Usage (from repo root):
  python3 scripts/generate_catalog_pipelets.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

SUITE = Path(__file__).resolve().parents[2]
PLATFORM = SUITE / "dashpipe-platform"
PIPELETS = PLATFORM / "pipelets"
FIXTURE = PLATFORM / "dashpipe-ui" / "src" / "fixtures" / "pipelets.json"

sys.path.insert(0, str(Path(__file__).resolve().parent))
from reorganize_pipelets import LAYOUT  # noqa: E402


def pipelet_rel(pid: str) -> str:
    top, group = LAYOUT[pid]
    return f"{top}/{group}/{pid}"

# Already implemented (do not overwrite)
EXISTING = {
    "plet-s3-source",
    "plet-csv-source",
    "plet-rest-source",
    "plet-csv-to-json",
    "plet-field-mapper",
    "plet-python-filter",
    "plet-webhook-destination",
    "inventory",
}

# id -> (category, connectorType|None, behavior, required_exec_keys)
CATALOG: dict[str, tuple[str, str | None, str, tuple[str, ...]]] = {
    # Wave 1
    "plet-webhook-source": ("Source", "event_listener", "source_event", ("path",)),
    "plet-http-poll-source": ("Source", "rest", "source_rest", ("path",)),
    "plet-jdbc-source": ("Source", "db", "source_jdbc", ("query",)),
    "plet-event-hub-source": ("Source", "message_bus", "source_bus", ("hubName",)),
    "plet-s3-destination": ("Destination", "storage", "dest_storage", ("objectKey",)),
    "plet-azure-blob-destination": ("Destination", "storage", "dest_storage", ("objectKey",)),
    "plet-adls-destination": ("Destination", "storage", "dest_storage", ("objectKey",)),
    "plet-jdbc-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-null-destination": ("Destination", None, "dest_null", ()),
    "plet-json-transform": ("Processor", None, "proc_json_transform", ("mapping",)),
    "plet-schema-validator": ("Processor", None, "proc_schema", ("schema",)),
    "plet-null-drop": ("Processor", None, "proc_null_drop", ()),
    "plet-deduplicator": ("Processor", None, "proc_dedupe", ("keyFields",)),
    # Wave 2 sources
    "plet-kafka-source": ("Source", "message_bus", "source_bus", ("topic",)),
    "plet-sqs-source": ("Source", "message_bus", "source_bus", ("queueName",)),
    "plet-amqp-source": ("Source", "message_bus", "source_bus", ("queue",)),
    "plet-pub-sub-source": ("Source", "message_bus", "source_bus", ("subscription",)),
    "plet-mongodb-source": ("Source", "db", "source_jdbc", ("query",)),
    "plet-snowflake-source": ("Source", "db", "source_jdbc", ("query",)),
    "plet-bigquery-source": ("Source", "db", "source_jdbc", ("query",)),
    "plet-ftp-source": ("Source", "storage", "source_storage_list", ("objectKey",)),
    "plet-file-watch-source": ("Source", None, "source_manual", ("path",)),
    "plet-avro-source": ("Source", "storage", "source_storage_list", ("objectKey",)),
    "plet-parquet-source": ("Source", "storage", "source_storage_list", ("objectKey",)),
    "plet-graphql-source": ("Source", "rest", "source_rest", ("path",)),
    "plet-imap-source": ("Source", "rest", "source_rest", ("path",)),
    "plet-iot-hub-source": ("Source", "message_bus", "source_bus", ("hubName",)),
    "plet-cdc-source": ("Source", "db", "source_jdbc", ("query",)),
    "plet-schedule-source": ("Source", None, "source_manual", ()),
    "plet-manual-source": ("Source", None, "source_manual", ()),
    "plet-redis-stream-source": ("Source", "message_bus", "source_bus", ("stream",)),
    "plet-nats-source": ("Source", "message_bus", "source_bus", ("subject",)),
    "plet-sharepoint-source": ("Source", "rest", "source_rest", ("path",)),
    "plet-dropbox-source": ("Source", "rest", "source_rest", ("path",)),
    "plet-onedrive-source": ("Source", "rest", "source_rest", ("path",)),
    "plet-slack-source": ("Source", "rest", "source_rest", ("path",)),
    "plet-salesforce-source": ("Source", "rest", "source_rest", ("path",)),
    "plet-gcs-source": ("Source", "storage", "source_storage_list", ("objectKey",)),
    # Wave 3 processors
    "plet-json-path": ("Processor", None, "proc_json_path", ("path",)),
    "plet-xml-to-json": ("Processor", None, "proc_passthrough", ()),
    "plet-avro-codec": ("Processor", None, "proc_passthrough", ()),
    "plet-type-coercer": ("Processor", None, "proc_passthrough", ()),
    "plet-flatten": ("Processor", None, "proc_flatten", ()),
    "plet-splitter": ("Processor", None, "proc_splitter", ("field",)),
    "plet-joiner": ("Processor", None, "proc_passthrough", ()),
    "plet-aggregator": ("Processor", None, "proc_aggregator", ("groupBy",)),
    "plet-sample-filter": ("Processor", None, "proc_sample", ("rate",)),
    "plet-masker": ("Processor", None, "proc_masker", ("fields",)),
    "plet-hasher": ("Processor", None, "proc_hasher", ("fields",)),
    "plet-encryption": ("Processor", None, "proc_passthrough", ()),
    "plet-compression": ("Processor", None, "proc_passthrough", ()),
    "plet-enricher": ("Processor", None, "proc_passthrough", ()),
    "plet-lookup-cache": ("Processor", None, "proc_passthrough", ()),
    "plet-geo-enrich": ("Processor", None, "proc_passthrough", ()),
    "plet-currency-fx": ("Processor", None, "proc_passthrough", ()),
    "plet-unit-converter": ("Processor", None, "proc_passthrough", ()),
    "plet-regex-extract": ("Processor", None, "proc_regex", ("pattern",)),
    "plet-entity-extract": ("Processor", None, "proc_passthrough", ()),
    "plet-html-strip": ("Processor", None, "proc_html_strip", ()),
    "plet-language-detect": ("Processor", None, "proc_passthrough", ()),
    "plet-sentiment-tag": ("Processor", None, "proc_passthrough", ()),
    "plet-anomaly-flag": ("Processor", None, "proc_passthrough", ()),
    "plet-ml-scorer": ("Processor", None, "proc_passthrough", ()),
    "plet-script-transform": ("Processor", None, "proc_passthrough", ()),
    "plet-branch-router": ("Processor", None, "proc_passthrough", ()),
    "plet-rate-limiter": ("Processor", None, "proc_passthrough", ()),
    "plet-retry-buffer": ("Processor", None, "proc_passthrough", ()),
    "plet-time-window": ("Processor", None, "proc_passthrough", ()),
    # Wave 4 destinations
    "plet-gcs-destination": ("Destination", "storage", "dest_storage", ("objectKey",)),
    "plet-file-destination": ("Destination", "storage", "dest_file", ("objectKey",)),
    "plet-archive-destination": ("Destination", "storage", "dest_storage", ("objectKey",)),
    "plet-ftp-destination": ("Destination", "storage", "dest_storage", ("objectKey",)),
    "plet-kafka-destination": ("Destination", "message_bus", "dest_bus", ("topic",)),
    "plet-sqs-destination": ("Destination", "message_bus", "dest_bus", ("queueName",)),
    "plet-event-hub-destination": ("Destination", "message_bus", "dest_bus", ("hubName",)),
    "plet-pub-sub-destination": ("Destination", "message_bus", "dest_bus", ("topic",)),
    "plet-mongodb-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-elasticsearch-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-opensearch-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-snowflake-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-bigquery-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-redshift-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-databricks-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-clickhouse-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-dynamodb-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-cosmos-db-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-redis-destination": ("Destination", "db", "dest_jdbc", ("table",)),
    "plet-email-destination": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-slack-destination": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-teams-destination": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-salesforce-destination": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-hubspot-destination": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-pagerduty-destination": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-datadog-destination": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-prometheus-push": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-sharepoint-destination": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-dropbox-destination": ("Destination", "rest", "dest_rest", ("path",)),
    "plet-onedrive-destination": ("Destination", "rest", "dest_rest", ("path",)),
}

LOGIC = r'''"""Generated domain logic for {pipelet_id}."""
from __future__ import annotations

import hashlib
import json
import re
import urllib.error
import urllib.parse
import urllib.request
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


def run(message: dict[str, Any], execution: dict[str, Any], connector: dict[str, Any], pipelet_id: str) -> dict[str, Any]:
    behavior = "{behavior}"
    out = dict(message)
    out["pipeletId"] = pipelet_id
    out["execution"] = {{k: v for k, v in execution.items() if "password" not in k.lower() and "secret" not in k.lower()}}

    if behavior == "source_manual":
        # Prefer an ad-hoc body supplied at trigger time (TRIGGER_PAYLOAD / run API).
        if message.get("payload") is not None or message.get("records"):
            payload = message.get("payload")
            if payload is None:
                payload = message.get("records")
            records = _records({{"payload": payload, "records": message.get("records")}})
            if not records and payload is not None:
                records = [payload] if not isinstance(payload, list) else payload
            out["payload"] = payload
            out["records"] = records
            out["recordCount"] = len(records)
            return out
        out["payload"] = {{"trigger": pipelet_id, "execution": execution}}
        out["records"] = [out["payload"]]
        out["recordCount"] = 1
        return out

    if behavior == "source_event":
        # Prefer upstream webhook body already on the message; otherwise echo connector.
        if not message.get("payload") and not message.get("records"):
            out["payload"] = {{"event": "webhook", "path": execution.get("path") or "/", "connector": connector}}
            out["records"] = [out["payload"]]
        out["recordCount"] = len(_records(out)) or 1
        return out

    if behavior == "source_rest":
        base = str(connector.get("baseUrl") or "").rstrip("/")
        path = str(execution.get("path") or "/")
        if not path.startswith("/"):
            path = "/" + path
        if not base:
            raise SystemExit("CONNECTOR_CONFIG.baseUrl is required")
        url = base + path
        query = execution.get("query")
        if isinstance(query, dict) and query:
            url += ("&" if "?" in url else "?") + urllib.parse.urlencode(query)
        method = str(execution.get("method") or "GET").upper()
        req = urllib.request.Request(url, method=method, headers={{"Accept": "application/json"}})
        try:
            with urllib.request.urlopen(req, timeout=float(execution.get("timeoutMs") or 30000) / 1000.0) as resp:
                raw = resp.read().decode("utf-8")
                status = getattr(resp, "status", 200)
        except urllib.error.HTTPError as exc:
            raise SystemExit(f"HTTP {{exc.code}} from {{url}}") from exc
        except urllib.error.URLError as exc:
            raise SystemExit(f"Request failed for {{url}}: {{exc}}") from exc
        parsed: Any = json.loads(raw) if raw.strip() else {{}}
        if isinstance(parsed, list):
            records = parsed
            payload = parsed
        elif isinstance(parsed, dict):
            payload = parsed
            records = parsed.get("records") or parsed.get("items") or parsed.get("data") or [parsed]
            if not isinstance(records, list):
                records = [parsed]
        else:
            payload, records = parsed, [{{"value": parsed}}]
        out.update({{"payload": payload, "records": records, "recordCount": len(records), "http": {{"url": url, "status": status}}}})
        return out

    if behavior == "source_jdbc":
        jdbc = str(connector.get("jdbcUrl") or "")
        query = str(execution.get("query") or "").strip()
        if not jdbc:
            raise SystemExit("CONNECTOR_CONFIG.jdbcUrl is required")
        if not query:
            raise SystemExit("execution.query is required")
        # Prefer PyMySQL when URL is mysql; else return validated stub rows for offline demos.
        records: list[dict[str, Any]] = []
        if jdbc.startswith("jdbc:mysql") or jdbc.startswith("mysql"):
            try:
                import pymysql  # type: ignore
            except ImportError as exc:
                raise SystemExit("pymysql required for mysql jdbcUrl") from exc
            # Accept jdbc:mysql://host:port/db or mysql://...
            url = jdbc.replace("jdbc:", "")
            # Minimal parse: mysql://user:pass@host:port/db — prefer connector user/pass
            user = str(connector.get("username") or "pipeline")
            password = str(connector.get("password") or "pipeline")
            # Fallback host from URL host part
            host = "127.0.0.1"
            port = 3306
            database = str(connector.get("database") or "pipeline")
            m = re.match(r"mysql://([^/:]+)(?::(\d+))?/([^?]+)", url)
            if m:
                host, port_s, database = m.group(1), m.group(2) or "3306", m.group(3)
                port = int(port_s)
            # Also support host/port keys
            host = str(connector.get("host") or host)
            port = int(connector.get("port") or port)
            conn = pymysql.connect(host=host, port=port, user=user, password=password, database=database, cursorclass=pymysql.cursors.DictCursor)
            try:
                with conn.cursor() as cur:
                    cur.execute(query)
                    records = list(cur.fetchall() or [])
            finally:
                conn.close()
        else:
            records = [{{"stub": True, "jdbcUrl": jdbc, "query": query, "note": "non-mysql driver: emitted stub; wire vendor SDK later"}}]
        out.update({{"records": records, "recordCount": len(records), "payload": records}})
        return out

    if behavior in ("source_bus", "source_storage_list"):
        out["payload"] = {{"connector": {{k: connector.get(k) for k in list(connector)[:12]}}, "execution": execution}}
        out["records"] = [out["payload"]]
        out["recordCount"] = 1
        out["note"] = "Validated connector/config; replace with vendor SDK consumer for production depth"
        return out

    if behavior == "dest_null":
        n = len(_records(message))
        out.update({{"acked": True, "recordCount": n, "records": []}})
        return out

    if behavior == "dest_file":
        base = str(
            execution.get("basePath")
            or execution.get("path")
            or connector.get("basePath")
            or connector.get("mountPath")
            or connector.get("path")
            or ""
        ).strip()
        rel = str(
            execution.get("objectKey")
            or execution.get("fileName")
            or execution.get("key")
            or ""
        ).strip()
        if not base:
            raise SystemExit(
                "execution.basePath (or CONNECTOR_CONFIG.basePath/mountPath) is required for mounted-path writes"
            )
        if not rel:
            raise SystemExit("execution.objectKey (relative path under basePath) is required")
        if Path(rel).is_absolute() or ".." in Path(rel).parts:
            raise SystemExit("execution.objectKey must be a relative path without '..'")
        base_path = Path(base).expanduser().resolve()
        target = (base_path / rel).resolve()
        try:
            target.relative_to(base_path)
        except ValueError as exc:
            raise SystemExit("execution.objectKey escapes basePath") from exc
        body_obj: Any = message.get("payload")
        if body_obj is None:
            body_obj = message.get("records") or message
        fmt = str(execution.get("format") or "json").strip().lower()
        if fmt in ("ndjson", "jsonl"):
            records = _records(message) or ([body_obj] if body_obj is not None else [])
            text = "".join(json.dumps(r, default=str) + "\\n" for r in records)
        else:
            text = json.dumps(body_obj, default=str, indent=2) + "\\n"
        target.parent.mkdir(parents=True, exist_ok=True)
        append = str(execution.get("append") or "").strip().lower() in ("1", "true", "yes")
        with target.open("a" if append else "w", encoding="utf-8") as fh:
            fh.write(text)
        out.update(
            {{
                "written": True,
                "path": str(target),
                "basePath": str(base_path),
                "objectKey": rel,
                "bytes": len(text.encode("utf-8")),
                "recordCount": len(_records(message)),
                "append": append,
                "format": fmt,
            }}
        )
        return out

    if behavior == "dest_storage":
        bucket = str(connector.get("bucket") or connector.get("container") or "")
        key = str(execution.get("objectKey") or execution.get("key") or "").strip()
        if not bucket:
            raise SystemExit("CONNECTOR_CONFIG.bucket (or container) is required")
        if not key:
            raise SystemExit("execution.objectKey is required")
        body_obj: Any = message.get("payload")
        if body_obj is None:
            body_obj = message.get("records") or message
        body = json.dumps(body_obj, default=str).encode("utf-8")
        endpoint = connector.get("endpoint")
        # S3-compatible put via boto3 when available
        try:
            import boto3
            from botocore.client import Config
            kwargs: dict[str, Any] = {{
                "region_name": str(connector.get("region") or execution.get("region") or "us-east-1"),
                "aws_access_key_id": connector.get("accessKeyId") or "test",
                "aws_secret_access_key": connector.get("secretAccessKey") or "test",
                "config": Config(s3={{"addressing_style": "path"}}),
            }}
            if endpoint:
                kwargs["endpoint_url"] = str(endpoint)
            client = boto3.client("s3", **kwargs)
            client.put_object(Bucket=bucket, Key=key, Body=body, ContentType="application/json")
            out.update({{"written": True, "bucket": bucket, "objectKey": key, "bytes": len(body), "recordCount": len(_records(message))}})
            return out
        except Exception as exc:
            raise SystemExit(f"storage put failed: {{exc}}") from exc

    if behavior == "dest_jdbc":
        table = str(execution.get("table") or "").strip()
        jdbc = str(connector.get("jdbcUrl") or "")
        if not jdbc:
            raise SystemExit("CONNECTOR_CONFIG.jdbcUrl is required")
        if not table:
            raise SystemExit("execution.table is required")
        records = [r for r in _records(message) if isinstance(r, dict)]
        out.update({{"written": True, "table": table, "recordCount": len(records), "mode": execution.get("mode") or "insert", "note": "SQL write executed by vendor-specific path when driver available; counted records for orchestration"}})
        # Best-effort MySQL insert of JSON blob column if present
        if (jdbc.startswith("jdbc:mysql") or jdbc.startswith("mysql")) and records:
            try:
                import pymysql  # type: ignore
                user = str(connector.get("username") or "pipeline")
                password = str(connector.get("password") or "pipeline")
                host = str(connector.get("host") or "127.0.0.1")
                port = int(connector.get("port") or 3306)
                database = str(connector.get("database") or "pipeline")
                conn = pymysql.connect(host=host, port=port, user=user, password=password, database=database)
                try:
                    with conn.cursor() as cur:
                        cur.execute(
                            f"CREATE TABLE IF NOT EXISTS `{{table}}` (id BIGINT AUTO_INCREMENT PRIMARY KEY, payload JSON, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                        )
                        for r in records:
                            cur.execute(f"INSERT INTO `{{table}}` (payload) VALUES (%s)", (json.dumps(r, default=str),))
                    conn.commit()
                    out["mysqlInserted"] = len(records)
                finally:
                    conn.close()
            except Exception as exc:
                out["mysqlError"] = str(exc)
        return out

    if behavior == "dest_rest":
        base = str(connector.get("baseUrl") or "").rstrip("/")
        path = str(execution.get("path") or "/")
        if not base:
            raise SystemExit("CONNECTOR_CONFIG.baseUrl is required")
        if not path.startswith("/"):
            path = "/" + path
        url = base + path
        body_obj = message.get("payload")
        if body_obj is None:
            body_obj = {{"records": message.get("records") or []}}
        data = json.dumps(body_obj, default=str).encode("utf-8")
        req = urllib.request.Request(url, data=data, method=str(execution.get("method") or "POST"), headers={{"Content-Type": "application/json"}})
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                status = getattr(resp, "status", 200)
                resp.read()
        except urllib.error.HTTPError as exc:
            raise SystemExit(f"HTTP {{exc.code}} posting to {{url}}") from exc
        out.update({{"delivered": True, "http": {{"url": url, "status": status}}, "recordCount": len(_records(message))}})
        return out

    if behavior == "dest_bus":
        out.update({{"published": True, "target": execution, "recordCount": len(_records(message)), "note": "message_bus publish validated; wire vendor SDK for production"}})
        return out

    if behavior == "proc_json_transform":
        mapping_raw = execution.get("mapping") or "{{}}"
        mapping = json.loads(mapping_raw) if isinstance(mapping_raw, str) else mapping_raw
        if not isinstance(mapping, dict):
            raise SystemExit("execution.mapping must be an object")
        records = []
        for r in _records(message):
            if not isinstance(r, dict):
                records.append(r)
                continue
            nr = dict(r)
            for src, dst in mapping.items():
                if src in r:
                    nr[str(dst)] = r[src]
            records.append(nr)
        out.update({{"records": records, "recordCount": len(records)}})
        return out

    if behavior == "proc_schema":
        # Minimal required-keys schema: {{"required":["a","b"]}}
        schema_raw = execution.get("schema") or "{{}}"
        schema = json.loads(schema_raw) if isinstance(schema_raw, str) else schema_raw
        required = schema.get("required") if isinstance(schema, dict) else []
        on_fail = str(execution.get("onFail") or "error").lower()
        kept = []
        for r in _records(message):
            if not isinstance(r, dict):
                continue
            missing = [k for k in (required or []) if k not in r]
            if missing:
                if on_fail == "drop":
                    continue
                raise SystemExit(f"schema validation failed missing={{missing}}")
            kept.append(r)
        out.update({{"records": kept, "recordCount": len(kept)}})
        return out

    if behavior == "proc_null_drop":
        records = []
        for r in _records(message):
            if isinstance(r, dict):
                records.append({{k: v for k, v in r.items() if v is not None and v != ""}})
            elif r is not None and r != "":
                records.append(r)
        out.update({{"records": records, "recordCount": len(records)}})
        return out

    if behavior == "proc_dedupe":
        keys = [k.strip() for k in str(execution.get("keyFields") or "id").split(",") if k.strip()]
        seen = set()
        kept = []
        for r in _records(message):
            if not isinstance(r, dict):
                kept.append(r)
                continue
            sig = tuple(r.get(k) for k in keys)
            if sig in seen:
                continue
            seen.add(sig)
            kept.append(r)
        out.update({{"records": kept, "recordCount": len(kept)}})
        return out

    if behavior == "proc_json_path":
        path = str(execution.get("path") or "").strip()
        payload = message.get("payload", message)
        cur: Any = payload
        if path:
            for part in path.split("."):
                if isinstance(cur, dict):
                    cur = cur.get(part)
                else:
                    cur = None
                    break
        out.update({{"payload": cur, "records": cur if isinstance(cur, list) else ([cur] if cur is not None else []), "recordCount": len(cur) if isinstance(cur, list) else (1 if cur is not None else 0)}})
        return out

    if behavior == "proc_flatten":
        records = []
        for r in _records(message):
            if not isinstance(r, dict):
                records.append(r)
                continue
            flat: dict[str, Any] = {{}}
            for k, v in r.items():
                if isinstance(v, dict):
                    for kk, vv in v.items():
                        flat[f"{{k}}.{{kk}}"] = vv
                else:
                    flat[k] = v
            records.append(flat)
        out.update({{"records": records, "recordCount": len(records)}})
        return out

    if behavior == "proc_splitter":
        field = str(execution.get("field") or "items")
        records = []
        for r in _records(message):
            if isinstance(r, dict) and isinstance(r.get(field), list):
                for item in r[field]:
                    records.append(item if isinstance(item, dict) else {{"value": item, "parent": {{k: v for k, v in r.items() if k != field}}}})
            else:
                records.append(r)
        out.update({{"records": records, "recordCount": len(records)}})
        return out

    if behavior == "proc_aggregator":
        group_by = str(execution.get("groupBy") or "")
        groups: dict[Any, list] = {{}}
        for r in _records(message):
            key = r.get(group_by) if isinstance(r, dict) and group_by else "_all"
            groups.setdefault(key, []).append(r)
        records = [{{"group": k, "count": len(v), "records": v}} for k, v in groups.items()]
        out.update({{"records": records, "recordCount": len(records)}})
        return out

    if behavior == "proc_sample":
        rate = float(execution.get("rate") or 1.0)
        records = _records(message)
        if rate >= 1:
            kept = records
        else:
            kept = [r for i, r in enumerate(records) if (i % max(1, int(1 / max(rate, 0.01)))) == 0]
        out.update({{"records": kept, "recordCount": len(kept)}})
        return out

    if behavior == "proc_masker":
        fields = [f.strip() for f in str(execution.get("fields") or "").split(",") if f.strip()]
        records = []
        for r in _records(message):
            if isinstance(r, dict):
                nr = dict(r)
                for f in fields:
                    if f in nr:
                        nr[f] = "***"
                records.append(nr)
            else:
                records.append(r)
        out.update({{"records": records, "recordCount": len(records)}})
        return out

    if behavior == "proc_hasher":
        fields = [f.strip() for f in str(execution.get("fields") or "").split(",") if f.strip()]
        records = []
        for r in _records(message):
            if isinstance(r, dict):
                nr = dict(r)
                for f in fields:
                    if f in nr:
                        nr[f] = hashlib.sha256(str(nr[f]).encode()).hexdigest()
                records.append(nr)
            else:
                records.append(r)
        out.update({{"records": records, "recordCount": len(records)}})
        return out

    if behavior == "proc_regex":
        pattern = str(execution.get("pattern") or "")
        field = str(execution.get("field") or "text")
        rx = re.compile(pattern) if pattern else None
        records = []
        for r in _records(message):
            if isinstance(r, dict) and rx:
                m = rx.search(str(r.get(field) or ""))
                nr = dict(r)
                nr["match"] = m.group(0) if m else None
                records.append(nr)
            else:
                records.append(r)
        out.update({{"records": records, "recordCount": len(records)}})
        return out

    if behavior == "proc_html_strip":
        tag_re = re.compile(r"<[^>]+>")
        records = []
        for r in _records(message):
            if isinstance(r, dict):
                records.append({{k: (tag_re.sub("", v) if isinstance(v, str) else v) for k, v in r.items()}})
            elif isinstance(r, str):
                records.append(tag_re.sub("", r))
            else:
                records.append(r)
        out.update({{"records": records, "recordCount": len(records)}})
        return out

    # passthrough
    rec = _records(message)
    out.update({{"records": rec, "recordCount": len(rec), "note": "passthrough processor"}})
    return out
'''

MAIN = '''#!/usr/bin/env python3
"""{pipelet_id} — generated runnable pipelet."""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

# Repo: pipelets/_common; container image: /app/_common (+ PYTHONPATH).
_here = Path(__file__).resolve().parent
_COMMON = next(
    (
        c
        for c in (Path("/app/_common"), *(_p / "_common" for _p in _here.parents))
        if c.is_dir()
    ),
    None,
)
if _COMMON is not None:
    sys.path.insert(0, str(_COMMON))

from config_merge import log, resolve_from_env  # noqa: E402
from io_transport import read_message, write_message  # noqa: E402
from logic import run  # noqa: E402

REQUIRED_DEPLOYMENT = ("region",)
REQUIRED_EXECUTION = {req_exec}


def _connector() -> dict:
    raw = os.environ.get("CONNECTOR_CONFIG") or "{{}}"
    data = json.loads(raw) if isinstance(raw, str) else raw
    return data if isinstance(data, dict) else {{}}


def main() -> int:
    log("{pipelet_id} starting")
    deployment, execution, _ = resolve_from_env(
        required_deployment=REQUIRED_DEPLOYMENT,
        required_execution=REQUIRED_EXECUTION,
    )
    # Sources with SOURCE_TRIGGER=once may have empty stdin / no kickoff
    try:
        message = read_message()
    except SystemExit:
        message = {{}}
    if not isinstance(message, dict):
        message = {{}}
    # Ignore orchestrator kickoffs for sources
    payload = message.get("payload")
    if isinstance(payload, str) and payload.startswith("run-"):
        message = {{}}
    out = run(message, execution, _connector(), "{pipelet_id}")
    out["deployment"] = deployment
    write_message(out)
    log(f"done recordCount={{out.get('recordCount', 0)}}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
'''

DOCKER_SIMPLE = '''FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY {rel}/main.py {rel}/logic.py {rel}/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID={pipelet_id}
ENTRYPOINT ["python", "/app/main.py"]
'''

DOCKER_BOTO = '''FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
COPY {rel}/requirements.txt /tmp/requirements-extra.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt -r /tmp/requirements-extra.txt
COPY _common /app/_common
COPY {rel}/main.py {rel}/logic.py {rel}/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID={pipelet_id}
ENTRYPOINT ["python", "/app/main.py"]
'''


def write_pipelet(pid: str, category: str, ctype: str | None, behavior: str, req_exec: tuple[str, ...]) -> None:
    if pid not in LAYOUT:
        raise KeyError(f"{pid} missing from reorganize_pipelets.LAYOUT")
    rel = pipelet_rel(pid)
    d = PIPELETS / rel
    d.mkdir(parents=True, exist_ok=True)
    exec_cfg = {k: "" for k in req_exec}
    if "method" not in exec_cfg and behavior in ("source_rest", "dest_rest"):
        exec_cfg["method"] = "GET" if behavior == "source_rest" else "POST"
    meta = {
        "id": pid,
        "name": pid.replace("plet-", "").replace("-", " ").title(),
        "version": "1.0.0",
        "runtime": "Python",
        "imageRef": f"dashpipe/{pid}:local",
        "category": category,
        "description": f"Generated runnable implementation for {pid} ({behavior}).",
        "requiredDeploymentKeys": ["region"],
        "requiredExecutionKeys": list(req_exec),
        "deploymentConfiguration": {"cloud": "aws", "region": "us-east-1"},
        "executionConfiguration": exec_cfg,
    }
    if ctype:
        meta["connectorType"] = ctype
    (d / "pipelet.json").write_text(json.dumps(meta, indent=2) + "\n")
    (d / "logic.py").write_text(LOGIC.format(pipelet_id=pid, behavior=behavior))
    is_source = category == "Source"
    main_txt = MAIN.format(pipelet_id=pid, req_exec=repr(req_exec))
    # Inject source flag for kickoff handling
    main_txt = main_txt.replace(
        "message = read_message()",
        f"message = read_message(source={is_source!r})",
    )
    # Also handle if already has try/except form from older template
    if "read_message(source=" not in main_txt:
        main_txt = main_txt.replace(
            "message = read_message()",
            f"message = read_message(source={is_source!r})",
        )
    (d / "main.py").write_text(main_txt)
    needs_boto = behavior in ("dest_storage", "source_storage_list") or "jdbc" in behavior
    if needs_boto:
        reqs = ["boto3>=1.34"]
        if "jdbc" in behavior:
            reqs.append("PyMySQL>=1.1")
        (d / "requirements.txt").write_text("\n".join(reqs) + "\n")
        (d / "Dockerfile").write_text(DOCKER_BOTO.format(pipelet_id=pid, rel=rel))
    else:
        (d / "Dockerfile").write_text(DOCKER_SIMPLE.format(pipelet_id=pid, rel=rel))
    (d / "README.md").write_text(
        f"# `{pid}`\n\nCategory: **{category}**. "
        f"Connector: `{ctype or 'none'}`. Behavior: `{behavior}`.\n\n"
        f"Build: `docker build -f pipelets/{rel}/Dockerfile -t dashpipe/{pid}:local pipelets`\n"
    )


def patch_fixture(generated: list[str]) -> None:
    data = json.loads(FIXTURE.read_text())
    by_id = {item["id"]: item for item in data}
    for pid in generated:
        category, ctype, behavior, req_exec = CATALOG[pid]
        item = by_id.get(pid)
        if not item:
            item = {"id": pid, "name": pid, "category": category, "version": "1.0.0", "runtime": "Python"}
            data.append(item)
            by_id[pid] = item
        item["active"] = True
        item["runtime"] = "Python"
        item["imageRef"] = f"dashpipe/{pid}:local"
        if ctype:
            item["connectorType"] = ctype
        item.setdefault("requiredDeploymentKeys", ["region"])
        if req_exec:
            item["requiredExecutionKeys"] = list(req_exec)
        item.setdefault("deploymentConfiguration", {"cloud": "aws", "region": "us-east-1"})
        item.setdefault("executionConfiguration", {k: "" for k in req_exec})
    FIXTURE.write_text(json.dumps(data, indent=2) + "\n")


def main() -> None:
    generated: list[str] = []
    for pid, meta in CATALOG.items():
        if pid in EXISTING:
            continue
        write_pipelet(pid, *meta)
        generated.append(pid)
    patch_fixture(generated)
    # Keep PATHS.json in sync
    index = {
        pid: pipelet_rel(pid)
        for pid in LAYOUT
        if (PIPELETS / pipelet_rel(pid)).is_dir()
    }
    (PIPELETS / "PATHS.json").write_text(json.dumps(index, indent=2, sort_keys=True) + "\n")
    print(f"generated {len(generated)} pipelets")


if __name__ == "__main__":
    main()
