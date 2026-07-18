#!/usr/bin/env python3
"""plet-webhook-destination — POST JSON body to connector baseUrl + path."""

from __future__ import annotations

import sys
from pathlib import Path

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
from webhook import run  # noqa: E402

REQUIRED_DEPLOYMENT = ("region",)
REQUIRED_EXECUTION = ("path",)


def main() -> int:
    log("plet-webhook-destination starting")
    deployment, execution, connector = resolve_from_env(
        required_deployment=REQUIRED_DEPLOYMENT,
        required_execution=REQUIRED_EXECUTION,
    )
    message = read_message(source=False)
    out = run(message, execution, connector)
    out["deployment"] = deployment
    write_message(out)
    record_count = out.get("recordCount")
    if record_count is None and isinstance(out.get("records"), list):
        record_count = len(out["records"])
    log(
        f"posted recordCount={record_count} to {out.get('http', {}).get('url')}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
