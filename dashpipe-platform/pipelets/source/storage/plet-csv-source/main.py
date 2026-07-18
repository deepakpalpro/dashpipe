#!/usr/bin/env python3
"""
plet-csv-source entrypoint.

Reads storage CONNECTOR_CONFIG (+ optional SERVICE_CONFIG) and step deployment/execution
config, fetches a CSV object, parses records, and emits one JSON message via IO_MODE.
"""

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

from config import resolve_from_env  # noqa: E402
from csv_source import fetch_and_parse  # noqa: E402
from io_transport import log, read_message, write_message  # noqa: E402


def main() -> int:
    log("plet-csv-source starting")
    read_message(source=True)
    cfg = resolve_from_env()
    log(
        f"resolved bucket={cfg.bucket} objectKey={cfg.object_key} "
        f"region={cfg.region} delimiter={cfg.delimiter!r} hasHeader={cfg.has_header} "
        f"endpoint={cfg.endpoint or '(default AWS)'}"
    )
    record = fetch_and_parse(cfg)
    write_message(record)
    log(f"emitted records={record['recordCount']} size={record['size']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
