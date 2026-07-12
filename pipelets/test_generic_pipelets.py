#!/usr/bin/env python3
"""Unit tests for the four generic processor/destination pipelets."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "_common"))
sys.path.insert(0, str(ROOT / "plet-csv-to-json"))
sys.path.insert(0, str(ROOT / "plet-python-filter"))
sys.path.insert(0, str(ROOT / "plet-field-mapper"))
sys.path.insert(0, str(ROOT / "plet-webhook-destination"))

from config_merge import resolve_layers  # noqa: E402
from filter_expr import filter_records, run as filter_run  # noqa: E402
from mapper import run as map_run  # noqa: E402
from transform import run as csv_run  # noqa: E402
from webhook import resolve_url  # noqa: E402


class CsvToJsonTest(unittest.TestCase):
    def test_parses_csv(self) -> None:
        out = csv_run(
            {"csv": "a,b\n1,2\n3,4\n"},
            {"delimiter": ",", "hasHeader": "true"},
        )
        self.assertEqual(out["recordCount"], 2)
        self.assertEqual(out["records"][0], {"a": "1", "b": "2"})

    def test_parses_s3_content_field(self) -> None:
        out = csv_run(
            {"content": "sku,qty\nA,1\n", "pipeletId": "plet-s3-source"},
            {"delimiter": ",", "hasHeader": "true"},
        )
        self.assertEqual(out["recordCount"], 1)

    def test_ignores_run_kickoff_payload(self) -> None:
        with self.assertRaises(SystemExit):
            csv_run(
                {"payload": "run-abc-123", "execution_id": "abc"},
                {"delimiter": ",", "hasHeader": "true"},
            )

    def test_requires_delimiter_via_layers(self) -> None:
        with self.assertRaises(SystemExit):
            resolve_layers(
                required_deployment=("region",),
                required_execution=("delimiter",),
                deployment={"region": "us-east-1"},
                execution={},
            )


class PythonFilterTest(unittest.TestCase):
    def test_expression_filter(self) -> None:
        records = [{"qty": 5}, {"qty": 0}, {"qty": 2}]
        kept = filter_records(records, "qty > 0")
        self.assertEqual(kept, [{"qty": 5}, {"qty": 2}])

    def test_rejects_unsafe_call(self) -> None:
        with self.assertRaises(Exception):
            filter_records([{"a": 1}], "__import__('os').system('x')")

    def test_run_requires_expression(self) -> None:
        with self.assertRaises(SystemExit):
            filter_run({"records": []}, {})


class FieldMapperTest(unittest.TestCase):
    def test_mapping_pairs(self) -> None:
        out = map_run(
            {"records": [{"unit_price": 1.5, "sku": "A"}]},
            {"mode": "map", "mapping": "unit_price=unitPrice,sku=sku"},
        )
        self.assertEqual(out["records"][0], {"unitPrice": 1.5, "sku": "A"})

    def test_wrap_mode(self) -> None:
        out = map_run(
            {"records": [{"sku": "A"}]},
            {"mode": "upsert", "mapping": "sku=sku"},
        )
        self.assertEqual(out["payload"]["mode"], "upsert")
        self.assertEqual(out["payload"]["items"][0]["sku"], "A")


class WebhookTest(unittest.TestCase):
    def test_url_join(self) -> None:
        url = resolve_url(
            {"baseUrl": "http://petstore:4010/api/v3"},
            {"path": "inventory/upload"},
        )
        self.assertEqual(url, "http://petstore:4010/api/v3/inventory/upload")

    def test_extract_prefers_petstore_payload(self) -> None:
        from webhook import extract_body

        body = extract_body(
            {
                "payload": "run-xyz",
                "petstorePayload": {"mode": "upsert", "items": [{"sku": "A"}]},
            },
            {},
        )
        self.assertEqual(body["items"][0]["sku"], "A")

    def test_extract_from_records(self) -> None:
        from webhook import extract_body

        body = extract_body({"records": [{"sku": "B"}]}, {})
        self.assertEqual(body["items"][0]["sku"], "B")


if __name__ == "__main__":
    unittest.main()
