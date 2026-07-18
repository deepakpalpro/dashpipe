# W1-US01 TDD Guide — Tenant CRUD + JWT tenant context

| Field | Value |
|-------|--------|
| **Story** | W1-US01 — Tenant CRUD + JWT/`tenant_id` request context |
| **Depends on** | Wave 0 complete |
| **Branch** | `W1-US01` from `wave-1` |
| **Timebox hint** | 1–2 days |
| **You will touch** | Flyway `V2__…`, Tenant entity/repo/service/controller, `TenantContext`, stub auth filter, fixtures `T001` |
| **Architecture refs** | §2.2 `tenants`, §3 APIs, §6.1 |
| **KB (create)** | `docs/delivery/kb/W1-US01-tenant-crud-context.md` |
| **Stakeholder TDD** | [`../../WAVE_1_TDD.md`](../../WAVE_1_TDD.md) |
| **AC source** | [`../../../waves/WAVE_1.md`](../../../waves/WAVE_1.md) § W1-US01 |

---

## 1. Overview

REST APIs to create/read/update/list tenants, plus a request-scoped **`TenantContext`** carrying `tenant_id` (JWT claim later; Wave 1 may stub via `X-Tenant-Id`).

**Done means:** Unit tests for validation + context; IT creates a tenant in MySQL and a follow-up request sees the same `tenant_id` in context.

**Out of scope:** Cross-tenant row filters (W1-US02); full IdP; connector/pipeline APIs.

---

## 2. Assumptions

| # | Assumption |
|---|------------|
| 1 | `wave-1` exists from Wave 0; Compose MySQL on `3306` |
| 2 | Stub auth is OK for `local`/`test` only — document real JWT later |
| 3 | Prefer JPA now (US02 needs Hibernate filters) |

```bash
git checkout wave-1 && git pull && git checkout -b W1-US01
docker compose up -d mysql
```

---

## 3. HLD / DFD

```mermaid
flowchart LR
  Client -->|POST_GET_PUT|/tenants| API[TenantController]
  API --> Svc[TenantService]
  Svc --> Repo[(tenants)]
  Client -->|X-Tenant-Id_or_JWT| Filter[TenantContextFilter]
  Filter --> Ctx[TenantContext]
```

Data flow: HTTP → filter sets `TenantContext` → controller/service → JPA → MySQL.

---

## 4. LLD

| Component | Responsibility |
|-----------|----------------|
| `Tenant` + `TenantRepository` | Persist tenants |
| `TenantService` / `TenantController` | CRUD under `/api/v1/tenants` |
| `TenantContext` | Thread/request-scoped `tenant_id` |
| `TenantContextFilter` | Stub: read `X-Tenant-Id` (or Bearer later) |
| Flyway `V2__…` | Extend schema if needed — **do not edit** applied `V1__` |

---

## 5. API interface

| Method | Path | Notes | Response |
|--------|------|-------|----------|
| `POST` | `/api/v1/tenants` | Create | `201` + id |
| `GET` | `/api/v1/tenants/{id}` | Get | `200` |
| `GET` | `/api/v1/tenants` | List (admin OK for now) | `200` |
| `PUT` | `/api/v1/tenants/{id}` | Update | `200` |

Auth stub: `X-Tenant-Id` header in `local`/`test`.

---

## 6. Testing

| Layer | Coverage | Tools |
|-------|----------|-------|
| Unit | Validation, context set/get/clear | JUnit, Mockito |
| Integration | Create/get round-trip; context populated | `@SpringBootTest`, Compose MySQL |
| Manual | curl create/get/duplicate slug | |

---

## 7. Risks

| Risk | Mitigation |
|------|------------|
| Editing applied Flyway | Always add `V2__…` |
| Real OAuth rabbit hole | Stub filter; ticket IdP later |
| Unit-only confidence | Require MySQL IT |

---

## 8. RED

| File | Method | Asserts |
|------|--------|---------|
| `TenantServiceTest` | `create_rejectsBlankSlug` | validation error |
| `TenantServiceTest` | `create_persistsAndReturnsId` | save called |
| `TenantContextTest` | `holdsTenantIdForCurrentThreadOrRequest` | set/get/clear |
| `TenantControllerIT` | `createAndGet_roundTrip` | POST 201; GET same slug |
| `TenantControllerIT` | `request_populatesTenantContext` | context has tenant id |

```bash
./mvnw -pl dashflow-api test -Dtest=TenantServiceTest,TenantContextTest,TenantControllerIT
```

**Stop.** Red.

---

## 9. GREEN

1. Migration if needed (`V2__…`).
2. JPA entity + repository.
3. Service + controller.
4. `TenantContext` + filter from stub header.
5. Fixture helper for `T001`.

### Checklist

- [ ] Tests green with MySQL up
- [ ] Slug unique enforced
- [ ] Stub auth marked `local`/`test` only
- [ ] No secrets in logs

---

## 10. REFACTOR

- Single place to read `TenantContext.getTenantId()`
- DTOs ≠ entity if needed
- Align JSON names with architecture

---

## 11. Docs & trackers

- [ ] KB: local create tenant + stub header
- [ ] Tracker · TEST_MATRIX · `WAVE_1.md` Done

| # | Action | Expected |
|---|--------|----------|
| 1 | POST tenant | 201 + id |
| 2 | GET by id | Same payload |
| 3 | Duplicate slug | 409/400 |

```text
merge → tag W1-US01 → delete → W1-US02 (do not skip isolation)
```

---

## 12. Common pitfalls

| Mistake | Fix |
|---------|-----|
| Editing `V1__baseline.sql` | Add `V2__…` |
| Tenant id only as method arg | Request-scoped `TenantContext` |
| Skipping IT | Persistence bugs hide in mocks |

## Help / escalate

- Architecture §2.2 · Wave 0 health IT pattern for Compose MySQL
