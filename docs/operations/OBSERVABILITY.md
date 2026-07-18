# Observability

Metrics, dashboards, and logs for Dashpipe pipelines.

## dashpipe-tools component

| Path | Purpose |
|------|---------|
| [prometheus/prometheus.yml](../../dashpipe-tools/prometheus/prometheus.yml) | Scrape config (API `/actuator/prometheus`) |
| [grafana/dashboards/](../../dashpipe-tools/grafana/dashboards/) | Pipeline Overview dashboard JSON |
| [grafana/provisioning/](../../dashpipe-tools/grafana/provisioning/) | Datasource and dashboard provisioning |
| [elk/logstash/pipeline/](../../dashpipe-tools/elk/logstash/pipeline/) | Logstash pipeline for execution logs |
| [k8s/](../../dashpipe-tools/k8s/) | Prometheus and Grafana manifests for AKS overlay |

## Enable locally

```bash
./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics   # Prometheus :9090, Grafana :3000
./dashpipe-ci_cd/scripts/localdev.sh start --with-elk       # Elasticsearch :9200, Kibana :5601
```

Or via Compose directly:

```bash
docker compose --profile metrics up -d
docker compose --profile elk up -d
```

## Smoke tests

```bash
./dashpipe-ci_cd/scripts/smoke-metrics.sh
./dashpipe-ci_cd/scripts/smoke-elk.sh
```

## Grafana

| Setting | Value |
|---------|-------|
| URL | http://localhost:3000 |
| Login | admin / admin |
| Dashboard | Pipeline Overview |

Set `PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL=http://localhost:3000` when starting the API (localdev does this automatically with `--with-metrics`).

The UI observability panels link to Grafana when this URL is configured.

## Platform observability API

The control plane exposes tenant-scoped endpoints:

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/observability/pipelines/{id}/completeness` | Completeness ratio |
| `GET /api/v1/observability/pipelines/{id}/latency` | Stage latency |
| `GET /api/v1/observability/pipelines/{id}/errors` | Error summary |
| `GET /api/v1/observability/executions/{id}/logs` | Indexed execution logs |

MCP tool `debug_execution` composes execution detail + logs + errors for agent debugging.

## ELK notes

- Local ELK is optional (Compose profile `elk`)
- CI uses an in-memory log indexer; ELK is for manual Kibana exploration
- ELK is not yet deployed in the Azure K8s overlay

See [W4-US04 KB](../../dashpipe-platform/docs/delivery/kb/W4-US04-elk-logs.md) for indexing conventions.
