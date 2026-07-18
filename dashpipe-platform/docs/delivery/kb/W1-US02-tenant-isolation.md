# KB: Tenant isolation filters (Wave 1)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W1-US02 / W1-US02 |
| **Audience** | Platform engineers / security reviewers |
| **Product area** | Tenancy / Data isolation |

## Prerequisites

- W1-US01 tenant CRUD + `X-Tenant-Id` stub context
- Compose MySQL; Flyway through `V2__tenant_notes.sql`

## Feature overview

Tenant-owned rows are filtered at the **persistence** layer with Hibernate `@Filter` (`tenantFilter`).

Wave 1 proves this with a minimal owned resource: **`tenant_notes`** (`/api/v1/tenant-notes`). Later connectors/services reuse the same `@TenantOwned` + filter pattern.

**Fail closed:** missing `X-Tenant-Id` on note APIs → `401`.

**Important:** Prefer JPQL (`findFilteredById`) over `EntityManager.find` / `findById` — Hibernate filters apply reliably to queries.

## How to verify

### Negative (must pass)

1. Create tenants A and B.
2. As B, `POST /api/v1/tenant-notes` with `X-Tenant-Id: <B>`.
3. As A, `GET /api/v1/tenant-notes/<id>` with `X-Tenant-Id: <A>` → **404** (never B’s payload).

### Positive

As B, GET the same id → **200**.

### Tests

```bash
docker compose up -d mysql
./mvnw -pl dashpipe-api test -Dtest=TenantIsolationIT,TenantNoteServiceTest
```

## Manual curl sketch

```bash
# create tenants via /api/v1/tenants, then:
curl -s -X POST localhost:8080/api/v1/tenant-notes \
  -H 'Content-Type: application/json' -H 'X-Tenant-Id: <B_ID>' \
  -d '{"title":"secret","body":"b"}'

curl -s -o /dev/null -w '%{http_code}\n' \
  localhost:8080/api/v1/tenant-notes/<NOTE_ID> -H 'X-Tenant-Id: <A_ID>'
# expect 404
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Cross-tenant GET returns 200 | Filter not enabled / used `findById` | Enable filter; use filtered JPQL |
| 401 on notes | Missing header | Send `X-Tenant-Id` |
| Flyway stuck at v1 | Migration not applied | Restart app; check `flyway_schema_history` for v2 |

## Related

- Developer TDD: [`../tdd/stories/w1/W1-US02-tdd.md`](../tdd/stories/w1/W1-US02-tdd.md)
- Tenant CRUD: [`W1-US01-tenant-crud-context.md`](W1-US01-tenant-crud-context.md)
