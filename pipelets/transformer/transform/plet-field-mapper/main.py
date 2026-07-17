#!/usr/bin/env python3
"""plet-field-mapper — project/rename fields; optional wrap for bulk POST."""

from __future__ import annotations

import sys
from pathlib import Path

# Repo: pipelets/_common; container image: /app/_common (+ PYTHONPATH).
_here = Path(__file__).resolve().parent
_COMMON = next(
    (
        c
        for c in (Path("/app/_common"), *(_p / "_common" for _p in _here.parents))
        if c.is_dir()
    ),
    None,
)
if _COMMON is not None:
    sys.path.insert(0, str(_COMMON))

from config_merge import log, resolve_from_env  # noqa: E402
from io_transport import read_message, write_message  # noqa: E402
from mapper import run  # noqa: E402

REQUIRED_DEPLOYMENT = ("region",)
REQUIRED_EXECUTION = ("mode",)


def main() -> int:
    log("plet-field-mapper starting")
    deployment, execution, _ = resolve_from_env(
        required_deployment=REQUIRED_DEPLOYMENT,
        required_execution=REQUIRED_EXECUTION,
    )
    message = read_message(source=False)
    out = run(message, execution)
    out["deployment"] = deployment
    write_message(out)
    log(f"mapped records={out.get('recordCount', 0)} mode={execution.get('mode')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
