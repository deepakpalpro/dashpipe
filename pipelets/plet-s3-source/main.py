#!/usr/bin/env python3
"""
plet-s3-source entrypoint.

Reads CONNECTOR_CONFIG + SERVICE_CONFIG + DEPLOYMENT_CONFIG + EXECUTION_CONFIG,
validates required Keys (deployment.region, execution.objectKey, connector.bucket),
fetches the object, and emits a JSON record via IO_MODE (stdio or queue).
"""

from __future__ import annotations

import sys
from pathlib import Path

# Shared helpers live under pipelets/_common (Docker PYTHONPATH or local parents).
_COMMON = Path(__file__).resolve().parents[1] / "_common"
if _COMMON.is_dir():
    sys.path.insert(0, str(_COMMON))

from config import resolve_from_env  # noqa: E402
from io_transport import log, read_message, write_message  # noqa: E402
from s3_source import fetch_object  # noqa: E402


def main() -> int:
    log("plet-s3-source starting")
    # Kickoff / empty stdin for sources
    read_message(source=True)
    cfg = resolve_from_env()
    log(
        f"resolved bucket={cfg.bucket} objectKey={cfg.object_key} "
        f"region={cfg.region} endpoint={cfg.endpoint or '(default AWS)'}"
    )
    record = fetch_object(cfg)
    write_message(record)
    log(f"emitted object size={record['size']} encoding={record['contentEncoding']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
