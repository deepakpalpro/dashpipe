#!/usr/bin/env python3
"""Unit tests for plet-python-filter (safe expression)."""

from __future__ import annotations

import unittest

from filter_expr import UnsafeExpression, compile_expression, run


class FilterExprTest(unittest.TestCase):
    def test_keeps_matching(self) -> None:
        out = run(
            {"records": [{"qty": 5}, {"qty": 0}]},
            {"expression": "qty > 0"},
        )
        self.assertEqual(out["recordCount"], 1)
        self.assertEqual(out["filteredOut"], 1)

    def test_rejects_unsafe_call(self) -> None:
        with self.assertRaises(UnsafeExpression):
            compile_expression("__import__('os').system('x')")


if __name__ == "__main__":
    unittest.main()
