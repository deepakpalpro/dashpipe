#!/usr/bin/env python3
"""
plet-rest-source entrypoint.

GETs (or POSTs) JSON from a REST connector baseUrl + path and emits one message.
"""

from __future__ import annotations

import sys
from pathlib import Path

_COMMON = Path(__file__).resolve().parents[1] / "_common"
if _COMMON.is_dir():
    sys.path.insert(0, str(_COMMON))

from config import resolve_from_env  # noqa: E402
from io_transport import log, read_message, write_message  # noqa: E402
from rest_source import fetch  # noqa: E402


def main() -> int:
    log("plet-rest-source starting")
    read_message(source=True)
    cfg = resolve_from_env()
    log(
        f"resolved method={cfg.method} url={cfg.url} "
        f"timeoutSec={cfg.timeout_sec} region={cfg.deployment.get('region')}"
    )
    record = fetch(cfg)
    write_message(record)
    log(
        f"emitted recordCount={record.get('recordCount')} "
        f"status={record.get('http', {}).get('status')}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
