# KB: Webhook metering (Wave 3)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W3-US07 / W3-US07 |
| **Audience** | Platform engineers / billing / support |
| **Product area** | Webhook Ingress / Usage |

## Prerequisites

- W3-US01 accept path
- Optional: W3-US03 (idempotent replays do **not** re-meter)

## Feature overview

On **first successful** accept (after durable publish), ingress emits two usage dimensions (architecture §11.7):

| Dimension | Amount | Tags |
|-----------|--------|------|
| `platform.webhook_events` | `1` | `tenant_id`, `connector_id` |
| `data.bytes_in` | raw body length (bytes) | same |

Wave 3 sinks events into `StubUsageEventCollector` (in-memory). Wave 5 will persist / aggregate via the Usage Collector.

**Not metered:** 401/404 rejects; idempotent duplicate replays (same logical event).

Metering failures are logged only — they never turn a successful accept into a non-202.

## How to verify

```bash
./mvnw -pl dashpipe-api test -Dtest=WebhookMeteringTest,WebhookIngressServiceTest
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| No events after 202 | Emit before publish / early return | Emit only after publish on first claim |
| Double count on retry | Idempotency path | Dup returns before emit |
| Events on 401 | Hook too early | Emit after HMAC + claim + publish |

## Related

- Developer TDD: [`../tdd/stories/w3/W3-US07-tdd.md`](../tdd/stories/w3/W3-US07-tdd.md)
- Idempotency: [`W3-US03-webhook-idempotency.md`](W3-US03-webhook-idempotency.md)
- Architecture §11.7, §6.2
