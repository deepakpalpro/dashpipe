# KB: Webhook rate limit + broker 503 (Wave 3)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W3-US04 / W3-US04 |
| **Audience** | Platform engineers / tenant integrators / support |
| **Product area** | Webhook Ingress / Limits |

## Prerequisites

- W3-US01 accept path
- MySQL + RabbitMQ for ITs

## Feature overview

| Status | When | Sender action |
|--------|------|---------------|
| **202** | Under limit + publish OK | Done |
| **429** | Per-tenant rate exceeded | Retry after `Retry-After` / `retry_after_seconds` |
| **503** | RabbitMQ publish failed | Retry (event was **not** accepted) |

Rate limit is a fixed window per `tenantId` (config under `pipeline.webhook.rate-limit`). Checked in `WebhookController` before accept.

On publish failure, ingress releases the idempotency claim so a retry can succeed, and never returns 2xx.

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashpipe-api test -Dtest=WebhookRateLimitIT,WebhookRateLimiterTest,WebhookIngressServiceTest
```

## Config

```yaml
pipeline:
  webhook:
    rate-limit:
      enabled: true
      requests-per-window: 120
      window-seconds: 60
      retry-after-seconds: 60
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Unexpected 429 | Burst / low IT limit | Raise `requests-per-window` or wait for window |
| 503 with broker up | Topology / AMQP error | Check RabbitMQ logs; retry |
| Stuck after 503 | Claim not released | US04 releases claim on publish fail |

## Related

- Developer TDD: [`../tdd/stories/w3/W3-US04-tdd.md`](../tdd/stories/w3/W3-US04-tdd.md)
- Accept: [`W3-US01-webhook-ingress-accept.md`](W3-US01-webhook-ingress-accept.md)
- Architecture §11.4, §11.8
