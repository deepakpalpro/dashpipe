# KB: Pipelet metrics emit (Wave 4)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W4-US01 / W4-US01 |
| **Audience** | Platform engineers / SRE / support |
| **Product area** | Observability / Metrics |

## Prerequisites

- W0-US04 `/actuator/prometheus`
- Wave 2 stub stage worker (local processing path)

## Feature overview

`PipeletMetricsEmitter` records architecture §7.1 series:

| Metric | Type | Labels |
|--------|------|--------|
| `pipelet_records_in_total` | Counter | `tenant_id`, `pipeline_id`, `pipelet_id` |
| `pipelet_records_out_total` | Counter | same |
| `pipelet_processing_duration_seconds` | Histogram/Timer | same |

**Cardinality policy:** `execution_id` is **not** a label on these series (avoids unbounded Prometheus cardinality). Correlate executions via logs / completeness APIs (W4-US02, W4-US05).

Local stub: each `StubStageWorker` stage message emits in=1, out=1 plus a short processing duration.

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashpipe-api test -Dtest=PipeletMetricsEmitterTest,PrometheusEndpointIT
```

Manual:

```bash
./mvnw -pl dashpipe-api spring-boot:run -Dspring-boot.run.profiles=local
# after a pipeline run (or IT emit), then:
curl -s localhost:8080/actuator/prometheus | grep pipelet_records
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| No `pipelet_*` series | Nothing emitted yet | Run fixture / call emitter |
| Missing labels | Wrong scrape / old binary | Redeploy; confirm §7.1 names |
| Cardinality explosion | Someone added `execution_id` | Remove; use logs for exec correlation |

## Related

- Developer TDD: [`../tdd/stories/w4/W4-US01-tdd.md`](../tdd/stories/w4/W4-US01-tdd.md)
- Prometheus baseline: [`W0-US04-logging-prometheus.md`](W0-US04-logging-prometheus.md)
- Architecture §7.1
- Modeling (telemetry vs billing; Grafana panels): [`../../SERVICE_CONNECTOR_PIPELET_MODEL.md`](../../SERVICE_CONNECTOR_PIPELET_MODEL.md) §5
