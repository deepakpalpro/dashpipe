# dashpipe-tools

Observability stack configuration for dashpipe-suite.

## Contents

| Path | Purpose |
|------|---------|
| [prometheus/](prometheus/) | Scrape config targeting API `/actuator/prometheus` |
| [grafana/](grafana/) | Dashboards and provisioning |
| [elk/](elk/) | Logstash pipeline for execution logs |
| [k8s/](k8s/) | Prometheus and Grafana manifests for AKS overlay |
| [docker-compose.tools.yml](docker-compose.tools.yml) | Compose services (profiles: `metrics`, `elk`) |
| [scripts/](scripts/) | Smoke tests for metrics and ELK |

## Enable locally

From repo root:

```bash
docker compose --profile metrics up -d    # Prometheus :9090, Grafana :3000
docker compose --profile elk up -d        # Elasticsearch, Kibana, Logstash
```

Or via localdev:

```bash
./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics
./dashpipe-ci_cd/scripts/localdev.sh start --with-elk
```

## Smoke tests

```bash
./dashpipe-tools/scripts/smoke-metrics.sh
./dashpipe-tools/scripts/smoke-elk.sh
```

Wrappers also exist at `./dashpipe-ci_cd/scripts/smoke-*.sh`.

## Docs

- [Observability operations guide](../docs/operations/OBSERVABILITY.md)
- [Platform observability API](../dashpipe-platform/docs/ARCHITECTURE.md) (section 7)
