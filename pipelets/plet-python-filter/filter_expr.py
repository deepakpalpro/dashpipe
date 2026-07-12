"""Generic record filter using a safe expression over each record."""

from __future__ import annotations

import ast
from typing import Any


ALLOWED_NODES = (
    ast.Expression,
    ast.BoolOp,
    ast.UnaryOp,
    ast.Compare,
    ast.Name,
    ast.Load,
    ast.Constant,
    ast.And,
    ast.Or,
    ast.Not,
    ast.Eq,
    ast.NotEq,
    ast.Lt,
    ast.LtE,
    ast.Gt,
    ast.GtE,
    ast.In,
    ast.NotIn,
    ast.Is,
    ast.IsNot,
    ast.List,
    ast.Tuple,
    ast.Dict,
    ast.Subscript,
    ast.Slice,
    ast.Attribute,
    ast.Call,
)


ALLOWED_FUNCS = {"len", "str", "int", "float", "bool"}


class UnsafeExpression(ValueError):
    pass


def _validate(node: ast.AST) -> None:
    if not isinstance(node, ALLOWED_NODES):
        raise UnsafeExpression(f"Disallowed expression node: {type(node).__name__}")
    if isinstance(node, ast.Call):
        if not isinstance(node.func, ast.Name) or node.func.id not in ALLOWED_FUNCS:
            raise UnsafeExpression("Only len/str/int/float/bool calls are allowed")
    for child in ast.iter_child_nodes(node):
        _validate(child)


def compile_expression(expression: str):
    tree = ast.parse(expression, mode="eval")
    _validate(tree)
    return compile(tree, "<filter-expression>", "eval")


def eval_expression(code, record: dict[str, Any]) -> bool:
    env = {
        "record": record,
        "r": record,
        **{k: record.get(k) for k in record.keys() if isinstance(k, str) and k.isidentifier()},
        "len": len,
        "str": str,
        "int": int,
        "float": float,
        "bool": bool,
    }
    return bool(eval(code, {"__builtins__": {}}, env))  # noqa: S307 — validated AST


def filter_records(records: list[dict[str, Any]], expression: str) -> list[dict[str, Any]]:
    code = compile_expression(expression)
    kept: list[dict[str, Any]] = []
    for record in records:
        if not isinstance(record, dict):
            continue
        try:
            if eval_expression(code, record):
                kept.append(record)
        except Exception:
            # fail closed for a bad row when expression errors
            continue
    return kept


def run(message: dict[str, Any], execution: dict[str, Any]) -> dict[str, Any]:
    expression = str(execution.get("expression") or "").strip()
    if not expression:
        raise SystemExit("execution.expression is required")
    records = message.get("records") or []
    if not isinstance(records, list):
        raise SystemExit("message.records must be a list")
    kept = filter_records(records, expression)
    return {
        **message,
        "records": kept,
        "recordCount": len(kept),
        "filteredOut": len(records) - len(kept),
        "pipeletId": "plet-python-filter",
        "execution": {"expression": expression},
    }
