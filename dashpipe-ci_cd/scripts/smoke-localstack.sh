#!/usr/bin/env bash
# Delegate to dashpipe-demo LocalStack smoke test.
set -euo pipefail
SUITE="$(cd "$(dirname "$0")/../.." && pwd)"
exec "$SUITE/dashpipe-demo/scripts/smoke-localstack.sh" "$@"
