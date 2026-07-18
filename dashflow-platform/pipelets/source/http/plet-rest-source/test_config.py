#!/usr/bin/env python3
"""Unit tests for plet-rest-source config + payload normalize."""

from __future__ import annotations

import unittest
from unittest.mock import MagicMock, patch

from config import resolve_config
from rest_source import _normalize_payload, fetch


class ResolveConfigTest(unittest.TestCase):
    def test_merges_rest_connector(self) -> None:
        cfg = resolve_config(
            connector={
                "baseUrl": "http://host.docker.internal:4010/api/v3",
                "timeoutMs": 15000,
                "api_key": "demo",
            },
            deployment={"region": "us-east-1"},
            execution={"path": "/inventory/items", "method": "GET"},
        )
        self.assertEqual(cfg.base_url, "http://host.docker.internal:4010/api/v3")
        self.assertEqual(cfg.path, "/inventory/items")
        self.assertEqual(cfg.method, "GET")
        self.assertEqual(cfg.url, "http://host.docker.internal:4010/api/v3/inventory/items")
        self.assertEqual(cfg.timeout_sec, 15.0)
        self.assertEqual(cfg.headers.get("X-API-Key"), "demo")

    def test_missing_base_url_fails(self) -> None:
        with self.assertRaises(SystemExit) as ctx:
            resolve_config(
                connector={},
                deployment={"region": "us-east-1"},
                execution={"path": "/x"},
            )
        self.assertIn("connector.baseUrl", str(ctx.exception))

    def test_query_appended(self) -> None:
        cfg = resolve_config(
            connector={"baseUrl": "http://example.com"},
            deployment={"region": "us-east-1"},
            execution={"path": "/search", "query": {"q": "cats"}},
        )
        self.assertEqual(cfg.query.get("q"), "cats")


class NormalizePayloadTest(unittest.TestCase):
    def test_list_becomes_records(self) -> None:
        payload, records, count = _normalize_payload([{"a": 1}, {"a": 2}])
        self.assertEqual(count, 2)
        self.assertEqual(records[0]["a"], 1)

    def test_items_wrapper(self) -> None:
        payload, records, count = _normalize_payload({"items": [{"id": 1}]})
        self.assertEqual(count, 1)
        self.assertEqual(records[0]["id"], 1)


class FetchTest(unittest.TestCase):
    def test_fetch_json_array(self) -> None:
        cfg = resolve_config(
            connector={"baseUrl": "http://example.com"},
            deployment={"region": "us-east-1"},
            execution={"path": "/items"},
        )
        fake_resp = MagicMock()
        fake_resp.__enter__.return_value = fake_resp
        fake_resp.__exit__.return_value = False
        fake_resp.status = 200
        fake_resp.read.return_value = b'[{"sku":"A"}]'
        fake_resp.headers = {"Content-Type": "application/json"}

        with patch("urllib.request.urlopen", return_value=fake_resp):
            out = fetch(cfg)
        self.assertEqual(out["recordCount"], 1)
        self.assertEqual(out["records"][0]["sku"], "A")
        self.assertEqual(out["http"]["status"], 200)


if __name__ == "__main__":
    unittest.main()
