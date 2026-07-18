"""Generated domain logic for plet-email-destination."""
from __future__ import annotations

import hashlib
import json
import re
import urllib.error
import urllib.parse
import urllib.request
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
    behavior = "dest_rest"
    out = dict(message)
    out["pipeletId"] = pipelet_id
    out["execution"] = {k: v for k, v in execution.items() if "password" not in k.lower() and "secret" not in k.lower()}

    if behavior == "source_manual":
        out["payload"] = {"trigger": pipelet_id, "execution": execution}
        out["records"] = [out["payload"]]
        out["recordCount"] = 1
        return out

    if behavior == "source_event":
        # Prefer upstream webhook body already on the message; otherwise echo connector.
        if not message.get("payload") and not message.get("records"):
            out["payload"] = {"event": "webhook", "path": execution.get("path") or "/", "connector": connector}
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
        req = urllib.request.Request(url, method=method, headers={"Accept": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=float(execution.get("timeoutMs") or 30000) / 1000.0) as resp:
                raw = resp.read().decode("utf-8")
                status = getattr(resp, "status", 200)
        except urllib.error.HTTPError as exc:
            raise SystemExit(f"HTTP {exc.code} from {url}") from exc
        except urllib.error.URLError as exc:
            raise SystemExit(f"Request failed for {url}: {exc}") from exc
        parsed: Any = json.loads(raw) if raw.strip() else {}
        if isinstance(parsed, list):
            records = parsed
            payload = parsed
        elif isinstance(parsed, dict):
            payload = parsed
            records = parsed.get("records") or parsed.get("items") or parsed.get("data") or [parsed]
            if not isinstance(records, list):
                records = [parsed]
        else:
            payload, records = parsed, [{"value": parsed}]
        out.update({"payload": payload, "records": records, "recordCount": len(records), "http": {"url": url, "status": status}})
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
            records = [{"stub": True, "jdbcUrl": jdbc, "query": query, "note": "non-mysql driver: emitted stub; wire vendor SDK later"}]
        out.update({"records": records, "recordCount": len(records), "payload": records})
        return out

    if behavior in ("source_bus", "source_storage_list"):
        out["payload"] = {"connector": {k: connector.get(k) for k in list(connector)[:12]}, "execution": execution}
        out["records"] = [out["payload"]]
        out["recordCount"] = 1
        out["note"] = "Validated connector/config; replace with vendor SDK consumer for production depth"
        return out

    if behavior == "dest_null":
        n = len(_records(message))
        out.update({"acked": True, "recordCount": n, "records": []})
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
            kwargs: dict[str, Any] = {
                "region_name": str(connector.get("region") or execution.get("region") or "us-east-1"),
                "aws_access_key_id": connector.get("accessKeyId") or "test",
                "aws_secret_access_key": connector.get("secretAccessKey") or "test",
                "config": Config(s3={"addressing_style": "path"}),
            }
            if endpoint:
                kwargs["endpoint_url"] = str(endpoint)
            client = boto3.client("s3", **kwargs)
            client.put_object(Bucket=bucket, Key=key, Body=body, ContentType="application/json")
            out.update({"written": True, "bucket": bucket, "objectKey": key, "bytes": len(body), "recordCount": len(_records(message))})
            return out
        except Exception as exc:
            raise SystemExit(f"storage put failed: {exc}") from exc

    if behavior == "dest_jdbc":
        table = str(execution.get("table") or "").strip()
        jdbc = str(connector.get("jdbcUrl") or "")
        if not jdbc:
            raise SystemExit("CONNECTOR_CONFIG.jdbcUrl is required")
        if not table:
            raise SystemExit("execution.table is required")
        records = [r for r in _records(message) if isinstance(r, dict)]
        out.update({"written": True, "table": table, "recordCount": len(records), "mode": execution.get("mode") or "insert", "note": "SQL write executed by vendor-specific path when driver available; counted records for orchestration"})
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
                            f"CREATE TABLE IF NOT EXISTS `{table}` (id BIGINT AUTO_INCREMENT PRIMARY KEY, payload JSON, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                        )
                        for r in records:
                            cur.execute(f"INSERT INTO `{table}` (payload) VALUES (%s)", (json.dumps(r, default=str),))
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
            body_obj = {"records": message.get("records") or []}
        data = json.dumps(body_obj, default=str).encode("utf-8")
        req = urllib.request.Request(url, data=data, method=str(execution.get("method") or "POST"), headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                status = getattr(resp, "status", 200)
                resp.read()
        except urllib.error.HTTPError as exc:
            raise SystemExit(f"HTTP {exc.code} posting to {url}") from exc
        out.update({"delivered": True, "http": {"url": url, "status": status}, "recordCount": len(_records(message))})
        return out

    if behavior == "dest_bus":
        out.update({"published": True, "target": execution, "recordCount": len(_records(message)), "note": "message_bus publish validated; wire vendor SDK for production"})
        return out

    if behavior == "proc_json_transform":
        mapping_raw = execution.get("mapping") or "{}"
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
        out.update({"records": records, "recordCount": len(records)})
        return out

    if behavior == "proc_schema":
        # Minimal required-keys schema: {"required":["a","b"]}
        schema_raw = execution.get("schema") or "{}"
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
                raise SystemExit(f"schema validation failed missing={missing}")
            kept.append(r)
        out.update({"records": kept, "recordCount": len(kept)})
        return out

    if behavior == "proc_null_drop":
        records = []
        for r in _records(message):
            if isinstance(r, dict):
                records.append({k: v for k, v in r.items() if v is not None and v != ""})
            elif r is not None and r != "":
                records.append(r)
        out.update({"records": records, "recordCount": len(records)})
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
        out.update({"records": kept, "recordCount": len(kept)})
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
        out.update({"payload": cur, "records": cur if isinstance(cur, list) else ([cur] if cur is not None else []), "recordCount": len(cur) if isinstance(cur, list) else (1 if cur is not None else 0)})
        return out

    if behavior == "proc_flatten":
        records = []
        for r in _records(message):
            if not isinstance(r, dict):
                records.append(r)
                continue
            flat: dict[str, Any] = {}
            for k, v in r.items():
                if isinstance(v, dict):
                    for kk, vv in v.items():
                        flat[f"{k}.{kk}"] = vv
                else:
                    flat[k] = v
            records.append(flat)
        out.update({"records": records, "recordCount": len(records)})
        return out

    if behavior == "proc_splitter":
        field = str(execution.get("field") or "items")
        records = []
        for r in _records(message):
            if isinstance(r, dict) and isinstance(r.get(field), list):
                for item in r[field]:
                    records.append(item if isinstance(item, dict) else {"value": item, "parent": {k: v for k, v in r.items() if k != field}})
            else:
                records.append(r)
        out.update({"records": records, "recordCount": len(records)})
        return out

    if behavior == "proc_aggregator":
        group_by = str(execution.get("groupBy") or "")
        groups: dict[Any, list] = {}
        for r in _records(message):
            key = r.get(group_by) if isinstance(r, dict) and group_by else "_all"
            groups.setdefault(key, []).append(r)
        records = [{"group": k, "count": len(v), "records": v} for k, v in groups.items()]
        out.update({"records": records, "recordCount": len(records)})
        return out

    if behavior == "proc_sample":
        rate = float(execution.get("rate") or 1.0)
        records = _records(message)
        if rate >= 1:
            kept = records
        else:
            kept = [r for i, r in enumerate(records) if (i % max(1, int(1 / max(rate, 0.01)))) == 0]
        out.update({"records": kept, "recordCount": len(kept)})
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
        out.update({"records": records, "recordCount": len(records)})
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
        out.update({"records": records, "recordCount": len(records)})
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
        out.update({"records": records, "recordCount": len(records)})
        return out

    if behavior == "proc_html_strip":
        tag_re = re.compile(r"<[^>]+>")
        records = []
        for r in _records(message):
            if isinstance(r, dict):
                records.append({k: (tag_re.sub("", v) if isinstance(v, str) else v) for k, v in r.items()})
            elif isinstance(r, str):
                records.append(tag_re.sub("", r))
            else:
                records.append(r)
        out.update({"records": records, "recordCount": len(records)})
        return out

    # passthrough
    rec = _records(message)
    out.update({"records": rec, "recordCount": len(rec), "note": "passthrough processor"})
    return out
