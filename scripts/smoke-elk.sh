#!/usr/bin/env bash
# W4-US04: optional Compose ELK health smoke (profile elk).
# Default CI path is InMemoryPipelineLogIndexer + ElkLogSmokeTest — this script is for manual ELK.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Checking ELK profile containers"
if ! docker compose --profile elk ps --status running 2>/dev/null | grep -q df-elasticsearch; then
  echo "Elasticsearch not running. Start with:"
  echo "  docker compose --profile elk up -d"
  echo "Then re-run this script. (CI uses stub indexer — see docs/delivery/kb/W4-US04-elk-logs.md)"
  exit 0
fi

echo "==> Elasticsearch cluster health"
curl -fsS "http://127.0.0.1:9200/_cluster/health?pretty" | head -20

echo "==> Index naming reminder: pipeline-logs-{tenant_id}-{YYYY.MM.DD}"
echo "==> Kibana: http://127.0.0.1:5601  pattern pipeline-logs-{tenant}-*"
echo "==> ELK smoke OK (cluster reachable)"
