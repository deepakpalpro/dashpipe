#!/usr/bin/env bash
# Delegate to dashpipe-tools metrics smoke test.
set -euo pipefail
SUITE="$(cd "$(dirname "$0")/../.." && pwd)"
exec "$SUITE/dashpipe-tools/scripts/smoke-metrics.sh" "$@"
