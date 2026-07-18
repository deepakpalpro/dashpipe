#!/usr/bin/env python3
"""Unit tests for plet-csv-to-json transform."""

from __future__ import annotations

import unittest

from transform import extract_csv_text, parse_csv_text, run


class CsvToJsonTest(unittest.TestCase):
    def test_parse_header(self) -> None:
        rows = parse_csv_text("a,b\n1,2\n", delimiter=",", has_header=True)
        self.assertEqual(rows, [{"a": "1", "b": "2"}])

    def test_ignores_run_kickoff(self) -> None:
        self.assertEqual(extract_csv_text({"payload": "run-abc"}), "")

    def test_run_happy(self) -> None:
        out = run({"content": "sku,qty\nA,1\n"}, {"delimiter": ",", "hasHeader": "true"})
        self.assertEqual(out["recordCount"], 1)
        self.assertEqual(out["records"][0]["sku"], "A")


if __name__ == "__main__":
    unittest.main()
