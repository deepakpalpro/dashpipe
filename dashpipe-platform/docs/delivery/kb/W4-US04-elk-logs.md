# KB: ELK log path (Wave 4)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W4-US04 / W4-US04 |
| **Audience** | Platform engineers / SRE / support |
| **Product area** | Observability / Logging |

## Prerequisites

- W0-US04 structured Logstash JSON console logs
- Wave 2 stub stage worker (emits logs on each stage)

## Feature overview

Architecture §7.3 index naming:

```text
pipeline-logs-{tenant_id}-{YYYY.MM.DD}
```

Kibana pattern: `pipeline-logs-{tenant_id}-*`

| Path | Role |
|------|------|
| `PipelineLogDocument` | §7.3 JSON fields (`tenant_id`, `pipeline_id`, `execution_id`, …) |
| `PipelineLogEmitter` | Console JSON (MDC) + index via `PipelineLogIndexer` |
| `InMemoryPipelineLogIndexer` | **CI / default local** stub — query by `execution_id` |
| Compose `--profile elk` | Optional ES `:9200`, Kibana `:5601`, Logstash `:5044` |

**CI policy:** unit/smoke tests use the in-memory indexer (no ELK containers required). Full Compose ELK is manual.

**Stub fixture:** each `StubStageWorker` stage emits a log with the run’s `execution_id`.

## How to verify

### Always-on (CI)

```bash
./mvnw -pl dashpipe-api test -Dtest=PipelineLogIndexNamesTest,ElkLogSmokeTest
```

### Optional Compose ELK

```bash
docker compose --profile elk up -d
./scripts/smoke-elk.sh
# Kibana http://127.0.0.1:5601 — create index pattern pipeline-logs-*-*
```

Ports:

| Service | Host port |
|---------|-----------|
| Elasticsearch | 9200 |
| Kibana | 5601 |
| Logstash beats | 5044 |

## Finding logs for `exec-*`

1. **Stub / API path (Wave 4):** `InMemoryPipelineLogIndexer.findByExecutionId("exec-…")` (also exposed by W4-US05 `GET /api/v1/observability/executions/{id}/logs`).
2. **Kibana:** Discover → filter `execution_id: "exec-…"` on pattern `pipeline-logs-{tenant}-*`.
3. **Console:** Logstash JSON lines include MDC `execution_id` after a stub run.

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| No docs for execution | Worker not running / old binary | Confirm stub emit; redeploy W4-US04+ |
| Wrong index name | Tenant casing / date TZ | Names are lowercase tenant + UTC day |
| Compose ELK OOM | ES heap | Profile is optional; use stub for CI |
| Port conflict 9200/5601 | Other ES/Kibana | Stop other stacks or change ports |

## Related

- Developer TDD: [`../tdd/stories/w4/W4-US04-tdd.md`](../tdd/stories/w4/W4-US04-tdd.md)
- Structured logging baseline: [`W0-US04-logging-prometheus.md`](W0-US04-logging-prometheus.md)
- Architecture §7.3
