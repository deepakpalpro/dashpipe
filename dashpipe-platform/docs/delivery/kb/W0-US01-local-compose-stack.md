# KB: Local Compose stack (Wave 0)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W0-US01 / W0-US01 |
| **Audience** | Platform engineers / support (local env) |
| **Product area** | Foundation / LocalStack |

## Prerequisites

- Docker Desktop (or compatible engine)
- Repo checkout with `docker-compose.yml` (W0-US01)
- AWS CLI v2 **or** `awslocal` for LocalStack smoke

**Default ports:** MySQL `3306`, RabbitMQ `5672` / mgmt `15672`, LocalStack **host `4567`** (maps to container `4566`). If `4566` is free on your machine, run `LOCALSTACK_HOST_PORT=4566 docker compose up -d` and set `LOCALSTACK_ENDPOINT=http://localhost:4566`.

**Optional profiles:**

| Profile | Services | Ports |
|---------|----------|-------|
| `elk` | Elasticsearch, Kibana, Logstash | `9200`, `5601`, `5044` |
| `metrics` | Prometheus, Grafana | `9090`, `3000` (`admin` / `admin`) |
| `petstore` | Petstore mock API | `4010` |

```bash
docker compose --profile metrics up -d
./scripts/smoke-metrics.sh
# With API on :8080, Prometheus scrapes http://host.docker.internal:8080/actuator/prometheus
# Grafana: http://localhost:3000 — set PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL=http://localhost:3000 for UI links
```

## Feature overview

Local development depends on MySQL (metadata), RabbitMQ (messaging), and LocalStack (S3/SQS cloud emulation). This article describes how to start the stack and verify LocalStack.

## Happy-path dataflow

```mermaid
flowchart LR
  Dev[Developer] --> Compose[docker_compose]
  Compose --> MySQL[(MySQL_3306)]
  Compose --> RabbitMQ[RabbitMQ_5672_15672]
  Compose --> LS[LocalStack_4566]
  Dev --> Smoke[smoke_localstack_sh]
  Smoke --> LS
```

## How to verify

1. `docker compose up -d`
2. `docker compose ps` — services healthy
3. `./scripts/smoke-compose-deps.sh` — MySQL + RabbitMQ OK
4. `./scripts/smoke-localstack.sh` — exit 0 (S3 + SQS)
5. RabbitMQ management: http://localhost:15672 (`pipeline` / `pipeline`)

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Port in use (e.g. 4566) | `lsof -iTCP:4566 -sTCP:LISTEN` | Default LocalStack host port is **4567**; or free the port / set `LOCALSTACK_HOST_PORT` |
| LocalStack not ready | `docker compose logs localstack` | Wait for healthcheck; rerun smoke |
| Permission errors | Docker daemon running | Restart Docker Desktop |

## Escalation

If CI cannot reach LocalStack, run LocalStack-labeled jobs only on runners with Docker; Prefer Testcontainers for MySQL in unit/IT paths.
