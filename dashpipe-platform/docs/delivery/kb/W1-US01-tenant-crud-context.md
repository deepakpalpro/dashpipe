# KB: Tenant CRUD + stub tenant context (Wave 1)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W1-US01 / W1-US01 |
| **Audience** | Platform engineers |
| **Product area** | Tenancy / API |

## Prerequisites

- Compose MySQL running (`docker compose up -d mysql`)
- `dashpipe-api` with `local` profile
- Wave 0 Flyway baseline applied (`tenants` table)

## Feature overview

Wave 1 introduces tenant CRUD under `/api/v1/tenants` and a **stub** request context:

- Header `X-Tenant-Id` is copied into `TenantContext` for the duration of the request
- Real JWT / IdP resolution is **not** implemented yet (replace `TenantContextFilter` later)

Cross-tenant row filtering for owned resources is **W1-US02**.

## APIs

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/api/v1/tenants` | Create (`name`, `slug`, optional `status`) |
| `GET` | `/api/v1/tenants/{id}` | Get by id |
| `GET` | `/api/v1/tenants` | List all (admin-style for now) |
| `PUT` | `/api/v1/tenants/{id}` | Update name/status |
| `GET` | `/api/v1/tenants/_context` | Dev helper: echoes context tenant id |

Duplicate `slug` → HTTP `409`. Blank slug → `400`.

### UI (Wave 6+)

`dashpipe-ui` **Tenants** page (`/tenants`) lists `GET /api/v1/tenants` and registers via the create form. The header tenant picker uses the same list (falls back to T001/T002 if the API is down).

## How to verify

### Create + get

```bash
./mvnw -pl dashpipe-api spring-boot:run -Dspring-boot.run.profiles=local

curl -s -X POST http://localhost:8080/api/v1/tenants \
  -H 'Content-Type: application/json' \
  -d '{"name":"Demo Tenant","slug":"demo-local","status":"trial"}'

# use returned id:
curl -s http://localhost:8080/api/v1/tenants/<id>
```

### Stub context

```bash
curl -s http://localhost:8080/api/v1/tenants/_context -H 'X-Tenant-Id: T001'
# {"tenantId":"T001"}
```

### Tests

```bash
docker compose up -d mysql
./mvnw -pl dashpipe-api test -Dtest=TenantContextTest,TenantServiceTest,TenantControllerIT
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 409 on create | Slug already used | Pick unique slug |
| Context empty | Missing `X-Tenant-Id` | Send header (stub auth) |
| JPA validate fail on boot | Schema drift vs entity | Align with `V1__baseline.sql`; add `V2__` if needed |
| IT skipped | MySQL not on `:3306` | `docker compose up -d mysql` |

## Related

- Developer TDD: [`../tdd/stories/w1/W1-US01-tdd.md`](../tdd/stories/w1/W1-US01-tdd.md)
- Next: isolation filters [`../tdd/stories/w1/W1-US02-tdd.md`](../tdd/stories/w1/W1-US02-tdd.md)
- Flyway baseline: [`W0-US03-flyway-baseline.md`](W0-US03-flyway-baseline.md)
