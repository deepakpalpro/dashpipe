#!/usr/bin/env python3
"""Unit tests for plet-csv-source config + CSV parse."""

from __future__ import annotations

import unittest

from config import resolve_config
from csv_source import parse_csv_text


class ResolveConfigTest(unittest.TestCase):
    def test_merges_storage_connector_and_csv_options(self) -> None:
        cfg = resolve_config(
            connector={
                "bucket": "demo-csv-source",
                "endpoint": "http://localstack:4566",
                "accessKeyId": "test",
                "secretAccessKey": "test",
            },
            deployment={"region": "us-east-1"},
            execution={
                "objectKey": "data/items.csv",
                "delimiter": ";",
                "hasHeader": "false",
            },
        )
        self.assertEqual(cfg.bucket, "demo-csv-source")
        self.assertEqual(cfg.object_key, "data/items.csv")
        self.assertEqual(cfg.delimiter, ";")
        self.assertFalse(cfg.has_header)
        self.assertEqual(cfg.endpoint, "http://localstack:4566")

    def test_missing_bucket_fails(self) -> None:
        with self.assertRaises(SystemExit) as ctx:
            resolve_config(
                connector={},
                deployment={"region": "us-east-1"},
                execution={"objectKey": "a.csv"},
            )
        self.assertIn("connector.bucket", str(ctx.exception))


class ParseCsvTest(unittest.TestCase):
    def test_header_rows(self) -> None:
        text = "sku,qty\nA,1\nB,2\n"
        rows = parse_csv_text(text, delimiter=",", has_header=True)
        self.assertEqual(rows, [{"sku": "A", "qty": "1"}, {"sku": "B", "qty": "2"}])

    def test_no_header(self) -> None:
        text = "A,1\nB,2\n"
        rows = parse_csv_text(text, delimiter=",", has_header=False)
        self.assertEqual(rows[0]["col0"], "A")
        self.assertEqual(rows[0]["col1"], "1")


if __name__ == "__main__":
    unittest.main()
