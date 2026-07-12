#!/usr/bin/env python3
"""Inventory pipelet entrypoint.

Modes:
  RUN_MODE=batch     Full S3 → CSV → filter → map → Petstore upload (default)
  RUN_MODE=stage     Single pipelet stage; PIPELET_ID selects behavior; stdin/stdout JSON
"""

from __future__ import annotations

import json
import os
import sys

from common import log, run_batch, stage_dispatch


def main() -> int:
    mode = os.environ.get("RUN_MODE", "batch").strip().lower()
    pipelet_id = os.environ.get("PIPELET_ID", "inventory-batch").strip()

    log(f"pipelet start mode={mode} pipelet_id={pipelet_id}")

    if mode == "batch":
        result = run_batch()
        print(
            json.dumps(
                {
                    "ok": True,
                    "received": result["result"].get("received"),
                    "summary": result["result"].get("summary"),
                }
            )
        )
        return 0

    if mode == "stage":
        stage_dispatch(pipelet_id)
        return 0

    print(f"Unknown RUN_MODE={mode}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
