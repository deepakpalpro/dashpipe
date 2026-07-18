# KB: Usage event ingest (Wave 5)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W5-US01 / W5-US01 |
| **Audience** | Platform engineers / support / billing |
| **Product area** | Metering / Usage |

## Prerequisites

- Compose MySQL
- W3 webhook metering (`UsageEventEmitter`) or any caller of `UsageEventCollector`

## Feature overview

Durable sink for architecture §6.2 `usage_events`:

| Piece | Role |
|-------|------|
| Flyway `V14__usage_events.sql` | Table + indexes + unique `idempotency_key` |
| `UsageEventService` | Map event → row; skip insert when key exists |
| `PersistingUsageEventCollector` | `@Primary` `UsageEventCollector` → MySQL |
| `StubUsageEventCollector` | Unit-test only (not a Spring bean) |

Webhook accept still emits `platform.webhook_events` + `data.bytes_in`; those now land in MySQL.

**Idempotency:** Prefer an explicit `idempotencyKey` on `UsageEvent`. If absent, a stable hash is derived from tenant/dimension/ids/time/amount. Duplicate keys do **not** double-bill.

## How to verify

```bash
./mvnw -pl dashpipe-api test -Dtest=UsageEventServiceTest,UsageEventPersistIT,WebhookMeteringTest
```

SQL (after a webhook accept or IT emit):

```sql
SELECT tenant_id, dimension, quantity, connector_id, recorded_at, idempotency_key
FROM usage_events
WHERE tenant_id = '<tenant-uuid>'
ORDER BY recorded_at DESC
LIMIT 20;
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| No rows after webhook | Metering exception logged; accept still 202 | Check app logs; confirm V14 applied |
| Duplicate bills | Missing/unstable idempotency key | Set explicit key; unique constraint |
| Flyway fail | Old schema | `docker compose up -d mysql`; restart API |

## Related

- Developer TDD: [`../tdd/stories/w5/W5-US01-tdd.md`](../tdd/stories/w5/W5-US01-tdd.md)
- W3 webhook metering: [`W3-US07`](../tdd/stories/w3/W3-US07-tdd.md) (if present)
- Architecture §6.2
