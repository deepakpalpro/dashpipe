# KB: MeterAgent (Wave 5)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W5-US02 / W5-US02 |
| **Audience** | Platform engineers / billing / support |
| **Product area** | Metering / Pipelet usage |

## Prerequisites

- W5-US01 `usage_events` persist
- Wave 2 stub stage worker

## Feature overview

`MeterAgent` emits **billable** usage into `UsageEventCollector` (MySQL). This is **not** the same as W4 Prometheus pipelet metrics.

### Stub fixture dimensions (per stage)

| Dimension | Amount | When |
|-----------|--------|------|
| `data.records_processed` | `1` (stub) | Every stage with records &gt; 0 |
| `compute.vcpu_seconds` | `0.001` (fixed stub) | Every stage |
| `platform.pipeline_runs` | `1` | **Last** stage only |

Idempotency keys: `stage:{executionId}:{stageOrder}:{dimension}:{epochMillis}`.

**3-stage fixture run** → typically **7** events: 3× records + 3× vcpu + 1× pipeline_runs.

Webhook dimensions (`platform.webhook_events`, `data.bytes_in`) remain on the W3 ingress path.

## How to verify

```bash
./mvnw -pl dashflow-api test -Dtest=MeterAgentTest,PipelineRunIT
```

After a completed stub run:

```sql
SELECT dimension, quantity, execution_id, pipelet_id, idempotency_key
FROM usage_events
WHERE execution_id = '<exec-uuid>'
ORDER BY recorded_at;
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| No stage usage rows | Old binary / MeterAgent not wired | Redeploy W5-US02+ |
| Missing `pipeline_runs` | Run did not reach last stage | Confirm COMPLETED |
| Confused with Prometheus | Looking at `/actuator/prometheus` | Billing is `usage_events` table |

## Related

- Developer TDD: [`../tdd/stories/w5/W5-US02-tdd.md`](../tdd/stories/w5/W5-US02-tdd.md)
- Ingest: [`W5-US01-usage-ingest.md`](W5-US01-usage-ingest.md)
- Architecture §6.2
