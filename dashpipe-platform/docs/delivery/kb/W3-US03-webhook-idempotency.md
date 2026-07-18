# KB: Webhook idempotency (Wave 3)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W3-US03 / W3-US03 |
| **Audience** | Platform engineers / tenant integrators / support |
| **Product area** | Webhook Ingress / Deduplication |

## Prerequisites

- W3-US01 accept path
- W3-US02 HMAC (signature still required before idempotency check)
- MySQL table `webhook_idempotency_keys` (Flyway `V13__webhook_idempotency.sql`)

## Feature overview

Duplicate deliveries produce **one logical event**. Order on accept: validate → HMAC → idempotency → publish once.

| Key source | Rule |
|------------|------|
| `X-Webhook-Id` | Preferred when present (trimmed; max 128 chars) |
| Body hash | `hash:` + SHA-256 hex of **raw** body bytes when header absent |

Keys are scoped by **tenant_id + connector_id** (unique constraint). Duplicate POST → **202** with the **same** `event_id`; **no second RabbitMQ publish**.

TTL: rows store `expires_at` = created + **7 days**. Wave 3 does not run a purge job yet — rely on the unique constraint; expired-row cleanup is deferred.

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashpipe-api test -Dtest=WebhookIdempotencyTest,WebhookControllerIT
```

## Manual curl sketch

```bash
BODY='{"action":"opened"}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SIGNING_SECRET" | awk '{print $2}')
HDR=(-H "Content-Type: application/json" -H "X-Hub-Signature-256: sha256=$SIG" -H "X-Webhook-Id: abc-1")

curl -s -X POST "localhost:8080/api/v1/webhooks/$TENANT_ID/$CONNECTOR_ID" "${HDR[@]}" -d "$BODY"
# note event_id

curl -s -X POST "localhost:8080/api/v1/webhooks/$TENANT_ID/$CONNECTOR_ID" "${HDR[@]}" -d "$BODY"
# same event_id; queue depth should not increase by a second logical message
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Two `event_id`s for same delivery | Missing / changing `X-Webhook-Id` and body differs | Send stable `X-Webhook-Id`; hash path only helps identical bodies |
| Dup still publishes twice | Race before unique constraint | Concurrent losers return winner's `event_id` without publish |
| Cross-connector collision | Same header reused on another connector | Expected — keys are scoped per connector |
| Store growth | No purge job yet | Monitor table; future job on `expires_at` |

## Related

- Developer TDD: [`../tdd/stories/w3/W3-US03-tdd.md`](../tdd/stories/w3/W3-US03-tdd.md)
- Accept path: [`W3-US01-webhook-ingress-accept.md`](W3-US01-webhook-ingress-accept.md)
- Signature: [`W3-US02-webhook-signature.md`](W3-US02-webhook-signature.md)
- Architecture §11.4, §11.8
