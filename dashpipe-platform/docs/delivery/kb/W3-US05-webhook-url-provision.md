# KB: Provision webhook URL (Wave 3)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W3-US05 / W3-US05 |
| **Audience** | Platform engineers / tenant integrators / support |
| **Product area** | Webhook Ingress / Connector admin |

## Prerequisites

- W3-US01 public ingress path
- `event_listener` connector type (`ct-event-listener`) with `signing_secret` in config
- Admin APIs use stub `X-Tenant-Id`

## Feature overview

Tenant admins provision a **stable** public URL for an event-listener connector:

```text
POST /api/v1/connectors/{id}/webhook-url
X-Tenant-Id: <tenant>
```

Response (architecture §3.3):

| Field | Meaning |
|-------|---------|
| `webhook_url` | `{pipeline.ingress.base-url}/api/v1/webhooks/{tenantId}/{connectorId}` |
| `signing_secret` | Stub-encrypted (`encrypted:` prefix) — never raw |
| `signature_header` | From tenant Auth / StubAuth (default `X-Hub-Signature-256`) |
| `created_at` | Connector `created_at` (stable across re-provision) |

Re-POST returns the **same** URL. Non-`event_listener` → **400**. Other tenant’s connector → **404**.

External senders POST to `webhook_url` **without** `X-Tenant-Id` (public ingress).

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashpipe-api test -Dtest=WebhookUrlProvisionIT
```

## Manual curl sketch

```bash
# Admin provision (requires X-Tenant-Id)
curl -s -X POST "localhost:8080/api/v1/connectors/$CONNECTOR_ID/webhook-url" \
  -H "X-Tenant-Id: $TENANT_ID"
# note webhook_url + encrypted signing_secret

# External delivery (no X-Tenant-Id) — see W3-US01 / W3-US02 KBs
```

Config: `pipeline.ingress.base-url` (default `http://localhost:8080`).

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 400 on provision | Connector type not `event_listener` | Use `ct-event-listener` |
| 404 | Wrong tenant header or connector id | Match `X-Tenant-Id` to owner |
| 400 missing secret | Config lacks `signing_secret` | Set secret on create |
| Wrong host in URL | `pipeline.ingress.base-url` | Override for env |
| Raw secret in response | Bug | Must be `encrypted:...` |

## Related

- Developer TDD: [`../tdd/stories/w3/W3-US05-tdd.md`](../tdd/stories/w3/W3-US05-tdd.md)
- Accept: [`W3-US01-webhook-ingress-accept.md`](W3-US01-webhook-ingress-accept.md)
- Signature: [`W3-US02-webhook-signature.md`](W3-US02-webhook-signature.md)
- Architecture §3.3, §11.4
