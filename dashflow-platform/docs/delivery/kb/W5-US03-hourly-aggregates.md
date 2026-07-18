# KB: Hourly usage aggregates (Wave 5)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W5-US03 / W5-US03 |
| **Audience** | Platform engineers / billing / support |
| **Product area** | Metering / Aggregates |

## Prerequisites

- W5-US01 `usage_events` rows present
- Compose MySQL (Flyway `V15__usage_aggregates`)

## Feature overview

Rolls raw `usage_events` into hourly `usage_aggregates` (UTC buckets).

| Piece | Role |
|-------|------|
| Flyway `V15__usage_aggregates.sql` | Table + unique `(tenant, dimension, granularity, period_start)` |
| `UsageAggregateService` | SUM events in `[hour, hour+1)` → upsert quantity + stub cost |
| `UsageAggregateJob` | Cron `0 0 * * * *` UTC → previous complete hour |
| `UsageUnitPrices` | Stub §6.2 unit prices (shared with later billing APIs) |
| `Clock` bean | Injectable / fixed Instant in tests |

**Idempotency:** Re-running the same hour updates existing rows (same id); does not duplicate.

**Raw events are kept** for disputes — aggregates never delete `usage_events`.

### Stub costs (examples)

| Dimension | Unit price |
|-----------|------------|
| `compute.vcpu_seconds` | $0.005 |
| `data.records_processed` | $0.00001 |
| `platform.pipeline_runs` | $0.01 |
| `platform.webhook_events` | $0.000005 |
| `data.bytes_in` | $0.01 / GB (per-byte stub) |

## How to re-run an hour

From a shell / admin path (service method):

```text
UsageAggregateService.aggregateHour(Instant.parse("2026-07-09T14:00:00Z"))
```

Buckets are always **UTC truncated** to the hour. Events with `recorded_at` in `[14:00, 15:00)` land in that row.

Scheduled path uses `aggregatePreviousHour()` (previous complete UTC hour relative to `Clock`).

## How to verify

```bash
./mvnw -pl dashflow-api test -Dtest=UsageAggregateJobTest,UsageAggregateIT
```

```sql
SELECT tenant_id, dimension, period_start, period_end, total_quantity, total_cost
FROM usage_aggregates
WHERE tenant_id = '<tenant-uuid>'
  AND granularity = 'hourly'
ORDER BY period_start, dimension;
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Empty aggregates | No events in that UTC hour | Confirm `recorded_at`; re-run `aggregateHour` |
| Wrong hour | Local TZ assumption | Always UTC truncate |
| Duplicate rows | Unique key missing / old schema | Confirm V15 applied |
| Cost looks wrong | Stub prices | See `UsageUnitPrices` |

## Related

- Developer TDD: [`../tdd/stories/w5/W5-US03-tdd.md`](../tdd/stories/w5/W5-US03-tdd.md)
- Ingest: [`W5-US01-usage-ingest.md`](W5-US01-usage-ingest.md)
- Architecture §6.2 Aggregation Schedule
