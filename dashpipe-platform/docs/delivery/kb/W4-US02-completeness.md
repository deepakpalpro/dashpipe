# KB: Completeness ratio (Wave 4)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W4-US02 / W4-US02 |
| **Audience** | Platform engineers / SRE / support |
| **Product area** | Observability / Completeness |

## Prerequisites

- W4-US01 pipelet metrics emit
- Wave 2 execution path (`StubStageWorker` → `markCompleted`)

## Feature overview

Architecture §7.4:

```text
completeness_pct = (total_records_out / total_records_in) × 100
```

| Surface | Value |
|---------|--------|
| DB / execution API | `completeness_pct` on `pipeline_executions` / `GET .../executions/{id}` |
| Prometheus | `pipeline_completeness_ratio` (0–1), labels `tenant_id` + `pipeline_id` only |

**Zero `records_in`:** ratio and percent are `0` (Wave 4 policy).

**Cardinality:** gauge is latest ratio per pipeline (not per `execution_id`). Per-run percent lives on the execution row.

**Local stub fixture:** last stage calls `markCompleted(executionId, 1, 1)` → `100.00` / ratio `1.0`.

**Known fixture (unit):** `98/100` → percent `98.00`, ratio `0.98`.

## How to verify

```bash
./mvnw -pl dashpipe-api test -Dtest=CompletenessCalculatorTest,CompletenessMetricsPublisherTest,PipelineRunOrchestratorTest
```

After a completed stub run:

```bash
curl -s localhost:8080/actuator/prometheus | grep pipeline_completeness_ratio
# execution detail JSON includes "completeness_pct"
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| `completeness_pct` null | Execution not COMPLETED / old binary | Confirm `markCompleted` path |
| Gauge missing | No completed run yet | Complete a fixture execution |
| Wrong ratio | Stage vs pipeline totals mixed | Use pipeline totals passed into `markCompleted` |

## Related

- Developer TDD: [`../tdd/stories/w4/W4-US02-tdd.md`](../tdd/stories/w4/W4-US02-tdd.md)
- Pipelet metrics: [`W4-US01-pipelet-metrics.md`](W4-US01-pipelet-metrics.md)
- Architecture §7.4
- Modeling (Grafana completeness expectations): [`../../SERVICE_CONNECTOR_PIPELET_MODEL.md`](../../SERVICE_CONNECTOR_PIPELET_MODEL.md) §5
