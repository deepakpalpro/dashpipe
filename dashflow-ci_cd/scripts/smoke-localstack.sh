#!/usr/bin/env bash
# Delegate to dashflow-demo LocalStack smoke test.
set -euo pipefail
SUITE="$(cd "$(dirname "$0")/../.." && pwd)"
exec "$SUITE/dashflow-demo/scripts/smoke-localstack.sh" "$@"
