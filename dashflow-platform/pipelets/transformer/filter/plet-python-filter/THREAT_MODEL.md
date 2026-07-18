# Threat model — plet-python-filter

Execution `expression` is evaluated with a **restricted AST** (`filter_expr.py`):

- Allowed: comparisons, boolean ops, constants, `record`/`r` / field names, `len`/`str`/`int`/`float`/`bool`
- Disallowed: attribute imports, arbitrary calls, `eval`/`exec`, comprehensions that escape allowlist

Fail closed: bad rows that raise during eval are dropped. Empty expression fails the Job.
