#!/usr/bin/env python3
"""Smoke tests for Wave 1 generated logic modules."""

from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def load_logic(pid: str):
    path = ROOT / pid / "logic.py"
    spec = importlib.util.spec_from_file_location(f"{pid}_logic", path)
    assert spec and spec.loader
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


class Wave1LogicTest(unittest.TestCase):
    def test_dedupe(self) -> None:
        logic = load_logic("plet-deduplicator")
        out = logic.run(
            {"records": [{"id": 1}, {"id": 1}, {"id": 2}]},
            {"keyFields": "id"},
            {},
            "plet-deduplicator",
        )
        self.assertEqual(out["recordCount"], 2)

    def test_json_transform(self) -> None:
        logic = load_logic("plet-json-transform")
        out = logic.run(
            {"records": [{"a": 1}]},
            {"mapping": {"a": "A"}},
            {},
            "plet-json-transform",
        )
        self.assertEqual(out["records"][0]["A"], 1)

    def test_null_destination(self) -> None:
        logic = load_logic("plet-null-destination")
        out = logic.run({"records": [1, 2]}, {}, {}, "plet-null-destination")
        self.assertTrue(out.get("acked"))


if __name__ == "__main__":
    unittest.main()
