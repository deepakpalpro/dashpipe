# KB: Webhook ingress accept (Wave 3)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W3-US01 / W3-US01 |
| **Audience** | Platform engineers / tenant integrators / support |
| **Product area** | Webhook Ingress |

## Prerequisites

- Wave 2 complete (RabbitMQ + `QueueNaming`)
- Compose MySQL + RabbitMQ
- Event-listener connector type `ct-event-listener` (Flyway `V12`)
- Tenant + connector row exist

## Feature overview

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/api/v1/webhooks/{tenantId}/{connectorId}` | **Public ingress** — tenant is in the URL; no `X-Tenant-Id` |

Returns `202` with `event_id` and `queued_to` after durable publish to:

`tenant.{tenantId}.webhook.{connectorId}.in`

**Does not** start a pipelet Job (that is W3-US06).

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashflow-api test -Dtest=WebhookIngressServiceTest,WebhookControllerIT
```

## Manual curl sketch

```bash
# create tenant + event_listener connector via admin APIs (X-Tenant-Id), then:
curl -s -X POST "localhost:8080/api/v1/webhooks/$TENANT_ID/$CONNECTOR_ID" \
  -H "Content-Type: application/json" \
  -d '{"action":"opened"}'
```

Inspect RabbitMQ management for queue `tenant.$TENANT_ID.webhook.$CONNECTOR_ID.in`.

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 404 | Bad tenant or connector id | Create connector under that tenant |
| No message | RabbitMQ down | `docker compose up -d rabbitmq` |
| Job created on POST | Unexpected | US01 must not call Job client — file bug |

## Related

- Developer TDD: [`../tdd/stories/w3/W3-US01-tdd.md`](../tdd/stories/w3/W3-US01-tdd.md)
- Architecture §11 · Next: signature (W3-US02)
