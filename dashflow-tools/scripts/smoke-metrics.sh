#!/usr/bin/env bash
# Optional Compose Prometheus + Grafana health smoke (profile metrics).
set -euo pipefail

SUITE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SUITE"

echo "==> Checking metrics profile containers"
if ! curl -fsS "http://127.0.0.1:9090/-/healthy" >/dev/null 2>&1; then
  echo "Prometheus not running. Start with:"
  echo "  docker compose --profile metrics up -d"
  echo "Then re-run this script. (API scrape target: host.docker.internal:8080/actuator/prometheus)"
  exit 0
fi

echo "==> Prometheus healthy"

echo "==> Scrape target (dashflow-api @ host.docker.internal:8080)"
targets_json="$(curl -fsS "http://127.0.0.1:9090/api/v1/targets")"
echo "$targets_json" | head -c 400
echo
if ! printf '%s' "$targets_json" | grep '"health":"up"' >/dev/null; then
  echo "WARN: Prometheus cannot scrape the API yet."
  echo "  - API must listen on :8080 (curl http://127.0.0.1:8080/actuator/prometheus)"
  echo "  - On Rancher Desktop, recreate Prometheus after compose fix (no host-gateway pin):"
  echo "      docker compose --profile metrics up -d --force-recreate prometheus"
  echo "  - On Linux Docker Engine, add extra_hosts host.docker.internal:host-gateway"
else
  echo "==> Sample metric query"
  curl -fsS "http://127.0.0.1:9090/api/v1/query?query=pipeline_completeness_ratio" | head -c 300
  echo
fi

if curl -fsS "http://127.0.0.1:3000/api/health" >/dev/null 2>&1; then
  echo "==> Grafana healthy"
  curl -fsS "http://127.0.0.1:3000/api/health" | head -c 200
  echo
  echo "==> Grafana UI: http://127.0.0.1:3000  (admin / admin)"
  echo "    Dashboard: Dashboards → Pipeline Overview (local)"
  echo "==> Wire UI links: PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL=http://localhost:3000"
else
  echo "==> Grafana container not running yet (still starting?)"
fi

echo "==> Metrics smoke OK"
