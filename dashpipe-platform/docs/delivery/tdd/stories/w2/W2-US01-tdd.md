# W2-US01 TDD Guide — Pipeline CRUD + visibility/mode

| Field | Value |
|-------|--------|
| **Story** | W2-US01 — Pipeline CRUD (`visibility`, `execution_mode`) |
| **Depends on** | Wave 1 complete |
| **Branch** | `W2-US01` from `wave-2` |
| **Timebox hint** | 1–1.5 days |
| **You will touch** | Flyway `pipelines` table, entity/repo/service/controller, tenant filter |
| **Architecture refs** | §2 `pipelines`, §3.1 |
| **KB (create)** | `docs/delivery/kb/W2-US01-pipeline-crud.md` |
| **Stakeholder TDD** | [`../../WAVE_2_TDD.md`](../../WAVE_2_TDD.md) |
| **AC source** | [`../../../waves/WAVE_2.md`](../../../waves/WAVE_2.md) § W2-US01 |

---

## 1. Overview

REST APIs to **create / list / get / update / archive** pipelines for the current tenant. Each pipeline has `visibility` (`public`/`private`) and `execution_mode` (`async`/`sync`).

**Done means:** `PipelineControllerIT` green; cross-tenant GET → 404.

**Out of scope:** Steps, run, executions.

---

## 2. Assumptions

| # | Assumption |
|---|------------|
| 1 | `wave-2` exists from Wave 1; Compose MySQL on `3306` |
| 2 | Stub auth via `X-Tenant-Id` in `local`/`test` (reuse W1 pattern) |
| 3 | Tenant Hibernate filter pattern from W1-US02 is available |

```bash
git checkout wave-2 && git pull && git checkout -b W2-US01
docker compose up -d mysql
```

---

## 3. HLD / DFD

```mermaid
flowchart LR
  Client -->|POST_GET_PUT_DELETE|/pipelines| API[PipelineController]
  API --> Svc[PipelineService]
  Svc --> Repo[(pipelines)]
  Client -->|X-Tenant-Id| Filter[TenantContextFilter]
  Filter --> Ctx[TenantContext]
  Ctx --> Svc
```

Data flow: HTTP → filter sets `TenantContext` → controller/service → JPA with tenant filter → MySQL.

---

## 4. LLD

| Component | Responsibility |
|-----------|----------------|
| `Pipeline` + `PipelineRepository` | Persist pipelines (`visibility`, `execution_mode`, status) |
| `PipelineService` / `PipelineController` | CRUD under `/api/v1/pipelines` |
| `@TenantOwned` + Hibernate filter | Reuse US02 pattern; single `@FilterDef` |
| Flyway migration | `pipelines` table — architecture columns needed for CRUD |

---

## 5. API interface

| Method | Path | Notes | Response |
|--------|------|-------|----------|
| `POST` | `/api/v1/pipelines` | Create | `201` + id |
| `GET` | `/api/v1/pipelines` | List for current tenant | `200` |
| `GET` | `/api/v1/pipelines/{id}` | Get (other tenant → 404) | `200` / `404` |
| `PUT` | `/api/v1/pipelines/{id}` | Update | `200` |
| `DELETE` | `/api/v1/pipelines/{id}` | Archive (`status=archived`) | `200` |

Auth stub: `X-Tenant-Id` header in `local`/`test`.

---

## 6. Testing

| Layer | Coverage | Tools |
|-------|----------|-------|
| Unit | Defaults (`draft` / `private` / `async`), validation | JUnit, Mockito |
| Integration | Create/get round-trip; cross-tenant 404 | `@SpringBootTest`, Compose MySQL |
| Manual | curl create/get/archive; other-tenant GET | |

---

## 7. Risks

| Risk | Mitigation |
|------|------------|
| Missing tenant filter | Same as W1-US02 — filter at Session |
| Hard-deleting rows | Prefer archive status |
| Implementing steps here | Defer to US02 |

---

## 8. RED

| File | Method | Asserts |
|------|--------|---------|
| `PipelineServiceTest` | `create_defaultsDraftAsyncPrivate` | enums / defaults |
| `PipelineControllerIT` | `createAndGet_asTenant` | 201 then 200 |
| `PipelineControllerIT` | `get_asOtherTenant_returnsNotFound` | 404 |

```bash
./mvnw -pl dashpipe-api test -Dtest=PipelineServiceTest,PipelineControllerIT
```

**Stop.** Red.

---

## 9. GREEN

1. Migration for `pipelines` (architecture columns needed for CRUD).
2. `@TenantOwned` + Hibernate filter (reuse US02 pattern; single `@FilterDef`).
3. Controller under `/api/v1/pipelines` with `X-Tenant-Id`.

### Checklist

- [ ] Unique name per tenant
- [ ] Archive sets `status=archived` (or soft-delete convention)
- [ ] Isolation IT
- [ ] Tests green with MySQL up

---

## 10. REFACTOR

- Align JSON field names with architecture create response
- Prepare for US02 steps relation (empty list OK)
- Prefer filtered JPQL (`findFilteredById`) over `findById`

---

## 11. Docs & trackers

- [ ] KB: local create pipeline + stub header + DELETE = archive
- [ ] Tracker · TEST_MATRIX · `WAVE_2.md` Done

| # | Action | Expected |
|---|--------|----------|
| 1 | POST pipeline | 201 with id; defaults `draft` / `private` / `async` |
| 2 | GET as other tenant | 404 |
| 3 | DELETE pipeline | `status=archived` (not hard-deleted) |

```text
merge → tag W2-US01 → delete → W2-US02
```

---

## 12. Common pitfalls

| Mistake | Fix |
|---------|-----|
| Missing tenant filter | Same as W1-US02 |
| Hard-deleting rows | Prefer archive status |
| Implementing steps here | Defer to US02 |

## Help / escalate

- Architecture §2 `pipelines`, §3.1 · W1-US02 tenant filter pattern
