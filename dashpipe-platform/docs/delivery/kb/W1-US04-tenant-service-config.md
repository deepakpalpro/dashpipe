# KB: Tenant service config (Wave 1)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W1-US04 / W1-US04 |
| **Audience** | Platform engineers / security reviewers |
| **Product area** | Services / Auth config |

## Prerequisites

- W1-US03 service types + StubAuth defaults (`st-auth`)
- Compose MySQL; Flyway through `V4__tenant_services.sql`
- Stub tenant header `X-Tenant-Id` (W1-US01)

## Feature overview

Tenants create **service instances** under `/api/v1/services` (Auth pattern first). Each row stores vendor + `tenant_config` JSON and may **inherit** platform `default_config` from the service type catalog.

**Responses never echo secrets.** Known secret keys (`client_secret`, `*_secret`, `api_key`, …) are replaced with `***` via `SecretRedactor` after merge.

**At-rest crypto is a stub:** secret string values are prefixed with `encrypted:` before persist. Replace with KMS / envelope encryption before production. Do not log raw config.

**Isolation:** `services` uses the same Hibernate `tenantFilter` as `tenant_notes` (single `@FilterDef` on `TenantNote`; other entities only `@Filter`). Cross-tenant GET → **404**.

## How to verify

### Positive

1. Create tenant A.
2. `POST /api/v1/services` with `X-Tenant-Id: <A>`, body referencing `st-auth` / `StubAuth` and a `client_secret`.
3. Response `config.client_secret` is `***`; inherited fields (e.g. `issuer`) appear when `inheritsDefault` is true.
4. `GET /api/v1/services/{id}` as A → same redaction; body must not contain the raw secret.

### Negative

As tenant B, GET A’s service id → **404**.

### Tests

```bash
docker compose up -d mysql
./mvnw -pl dashpipe-api test -Dtest=TenantServiceConfigServiceTest,TenantServiceConfigIT
```

## Manual curl sketch

```bash
# after creating tenant TENANT_ID:
curl -s -X POST localhost:8080/api/v1/services \
  -H 'Content-Type: application/json' -H "X-Tenant-Id: $TENANT_ID" \
  -d '{"serviceTypeId":"st-auth","vendor":"StubAuth","name":"My Auth","tenantConfig":{"client_id":"c1","client_secret":"s3cret"},"inheritsDefault":true}'

curl -s localhost:8080/api/v1/services/<SERVICE_ID> -H "X-Tenant-Id: $TENANT_ID"
# expect client_secret: "***"
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Secret visible in JSON | Response mapped without redactor | Always map via `toResponse` / `SecretRedactor` |
| Cross-tenant GET 200 | Filter not enabled / `findById` | Enable filter; use `findFilteredById` |
| Duplicate `@FilterDef` boot failure | FilterDef on multiple entities | Keep one `@FilterDef`; others `@Filter` only |
| 400 unknown vendor | Vendor not in `service_defaults` | Use seeded `StubAuth` or register default |

## Related

- Developer TDD: [`../tdd/stories/w1/W1-US04-tdd.md`](../tdd/stories/w1/W1-US04-tdd.md)
- Service types: [`W1-US03-service-types.md`](W1-US03-service-types.md)
- Isolation: [`W1-US02-tenant-isolation.md`](W1-US02-tenant-isolation.md)
- Modeling (service vs connector vs step): [`../../SERVICE_CONNECTOR_PIPELET_MODEL.md`](../../SERVICE_CONNECTOR_PIPELET_MODEL.md)
