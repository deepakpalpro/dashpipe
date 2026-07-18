#!/usr/bin/env python3
"""Unit tests for plet-field-mapper."""

from __future__ import annotations

import unittest

from mapper import map_record, parse_mapping, run


class FieldMapperTest(unittest.TestCase):
    def test_parse_csv_pairs(self) -> None:
        self.assertEqual(parse_mapping("a=b,c=d"), {"a": "b", "c": "d"})

    def test_map_record(self) -> None:
        self.assertEqual(map_record({"a": 1, "x": 9}, {"a": "A"}), {"A": 1})

    def test_run(self) -> None:
        out = run(
            {"records": [{"unit_price": "1.5", "sku": "S"}]},
            {"mode": "map", "mapping": "unit_price=unitPrice,sku=sku"},
        )
        self.assertEqual(out["records"][0], {"unitPrice": "1.5", "sku": "S"})


if __name__ == "__main__":
    unittest.main()
