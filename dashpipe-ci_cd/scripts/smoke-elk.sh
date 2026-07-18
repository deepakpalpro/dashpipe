#!/usr/bin/env bash
# Delegate to dashpipe-tools ELK smoke test.
set -euo pipefail
SUITE="$(cd "$(dirname "$0")/../.." && pwd)"
exec "$SUITE/dashpipe-tools/scripts/smoke-elk.sh" "$@"
