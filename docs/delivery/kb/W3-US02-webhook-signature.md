# KB: Webhook signature verification (Wave 3)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W3-US02 / W3-US02 |
| **Audience** | Platform engineers / tenant integrators / support |
| **Product area** | Webhook Ingress / Auth |

## Prerequisites

- W3-US01 accept path
- Event-listener connector with `signing_secret` in config
- StubAuth defaults include `signature_header` = `X-Hub-Signature-256` (Flyway V3)

## Feature overview

Ingress verifies GitHub-style HMAC **before** publish:

```text
X-Hub-Signature-256: sha256=<hex(HMAC_SHA256(signing_secret, raw_body))>
```

| Source | Field |
|--------|--------|
| Connector config | `signing_secret` (HMAC key) |
| Auth service (StubAuth defaults / tenant override) | `signature_header` |

Invalid or missing signature → **401**; no RabbitMQ publish / topology declare.

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashflow-api test -Dtest=WebhookSignatureVerifierTest,WebhookControllerIT
```

## Manual curl sketch

```bash
BODY='{"action":"opened"}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SIGNING_SECRET" | awk '{print $2}')
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  "localhost:8080/api/v1/webhooks/$TENANT_ID/$CONNECTOR_ID" \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=$SIG" \
  -d "$BODY"
# expect 202

curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  "localhost:8080/api/v1/webhooks/$TENANT_ID/$CONNECTOR_ID" \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=deadbeef" \
  -d "$BODY"
# expect 401
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 401 always | Wrong secret or signed parsed JSON not raw bytes | Sign exact request body bytes |
| 401 missing header | Header name mismatch | Use Auth `signature_header` (default `X-Hub-Signature-256`) |
| Secret in logs | Bug | Never log `signing_secret` |

## Related

- Developer TDD: [`../tdd/stories/w3/W3-US02-tdd.md`](../tdd/stories/w3/W3-US02-tdd.md)
- Accept path: [`W3-US01-webhook-ingress-accept.md`](W3-US01-webhook-ingress-accept.md)
- Architecture §11.4
