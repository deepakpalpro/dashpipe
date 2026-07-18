#!/usr/bin/env bash
# Delegate to dashflow-tools metrics smoke test.
set -euo pipefail
SUITE="$(cd "$(dirname "$0")/../.." && pwd)"
exec "$SUITE/dashflow-tools/scripts/smoke-metrics.sh" "$@"
