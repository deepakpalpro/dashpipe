# KB: Pipeline CRUD (Wave 2)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W2-US01 / W2-US01 |
| **Audience** | Platform engineers / tenant admins |
| **Product area** | Pipelines |

## Prerequisites

- Wave 1 tenancy (`X-Tenant-Id` stub)
- Compose MySQL; Flyway through `V9__pipelines.sql`

## Feature overview

A **pipeline** owns an ordered list of **steps** (W2-US02). Each step configures a **pipelet** that will run as a Job/Pod on `POST .../run` — the pipeline row itself is metadata (`name`, `visibility`, `execution_mode`, `status`); the steps are the runtime graph.

Tenant-scoped pipelines under `/api/v1/pipelines`:

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/api/v1/pipelines` | Create (`draft`, default `private` + `async`) |
| `GET` | `/api/v1/pipelines` | List current tenant |
| `GET` | `/api/v1/pipelines/{id}` | Get (includes `steps` after W2-US02) |
| `PUT` | `/api/v1/pipelines/{id}` | Update metadata; bumps `version` |
| `DELETE` | `/api/v1/pipelines/{id}` | **Archive** (`status=archived`), does not hard-delete |

Isolation: Hibernate `tenantFilter` — cross-tenant GET → **404**.

## How to verify

```bash
docker compose up -d mysql
./mvnw -pl dashflow-api test -Dtest=PipelineServiceTest,PipelineControllerIT
```

## Manual curl sketch

```bash
curl -s -X POST localhost:8080/api/v1/pipelines \
  -H 'Content-Type: application/json' -H "X-Tenant-Id: $TENANT_ID" \
  -d '{"name":"customer-sync","description":"demo","visibility":"private","execution_mode":"async"}'
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 401 / missing tenant | No `X-Tenant-Id` | Send header |
| Cross-tenant 200 | Filter not enabled | Use filtered JPQL |
| Duplicate name 409 | Unique `(tenant_id, name)` | Rename |

## Related

- Developer TDD: [`../tdd/stories/w2/W2-US01-tdd.md`](../tdd/stories/w2/W2-US01-tdd.md)
- Execution plan: [`../waves/WAVE_2.md`](../waves/WAVE_2.md)
- Steps API: [`W2-US02-pipeline-steps.md`](W2-US02-pipeline-steps.md)
