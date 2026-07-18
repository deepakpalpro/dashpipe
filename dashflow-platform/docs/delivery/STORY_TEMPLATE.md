# User Story Template

Copy this file (or its sections) for every user story. **All acceptance-criteria sections are mandatory** before a story can be marked Done.

Use IDs from [`../DELIVERY_PLAN.md`](../DELIVERY_PLAN.md): `Wn-Fx-Ey-USzz`.

---

## Metadata

| Field | Value |
|-------|--------|
| **Story ID** | `W#-US##` |
| **Wave / Feature / Epic** | `W#` / `W#-F#` / `W#-F#-E#` |
| **Title** | Short imperative title |
| **Priority** | Must / Should / Could |
| **Dependencies** | Story IDs or infrastructure prerequisites |
| **Architecture refs** | e.g. [`ARCHITECTURE.md`](../ARCHITECTURE.md) §3.1, §11 |
| **Owner** | |
| **Status** | Todo / In Progress / Done |

---

## User story

**As a** \<role\>  
**I want** \<capability\>  
**so that** \<outcome\>.

### Scope

- In scope:
- Out of scope:

---

## Acceptance criteria (mandatory)

### 1. TDD

| Step | Action | Evidence |
|------|--------|----------|
| **Red** | List failing unit/integration tests written first (file + method names) | |
| **Green** | Minimal implementation that makes those tests pass | |
| **Refactor** | Clean-up steps without changing behavior; tests still green | |

- [ ] Failing tests committed/shown before production code for this story
- [ ] Tests remain green after refactor

### 2. Unit tests

| Class / method under test | Key assertions | Fixtures |
|---------------------------|----------------|----------|
| | | |

- [ ] Unit tests cover happy path and failure/edge cases listed below
- [ ] No real network, K8s, or cloud calls in unit tests

**Failure / edge cases:**

1.
2.

### 3. Integration tests

| Test name | Stack under test | Assertions |
|-----------|------------------|------------|
| | e.g. `@SpringBootTest` + Testcontainers MySQL / RabbitMQ | |

- [ ] Integration tests use Testcontainers or Compose test profile where persistence/messaging is required
- [ ] Tenant isolation verified when the story touches tenant-owned data

### 4. Mock data service

| Fixture / factory | Entity | Location (path) | Notes |
|-------------------|--------|-----------------|-------|
| | tenant / pipeline / connector / usage | `src/test/resources/...` | |

- [ ] Deterministic fixture IDs suitable for repeatable runs
- [ ] Documented how to reset or reload mock data

### 5. Mock server / LocalStack

| Dependency | Tool | Purpose | Config notes |
|------------|------|---------|--------------|
| External HTTP API | WireMock | | |
| S3 / SQS / etc. | LocalStack | | Endpoint `http://localhost:4566` (dev) |
| Auth / IdP | WireMock or stub Service | | |

- [ ] Stub mappings committed under test resources (or Compose volume)
- [ ] Story notes which services are **real** vs **mocked** in manual test

### 6. Manual test steps

**Preconditions:**

1.

**Steps:**

| # | Action | Expected result |
|---|--------|-----------------|
| 1 | | |
| 2 | | |
| 3 | | |

**Teardown:**

1.

- [ ] Manual steps executed successfully on local Compose stack
- [ ] Screenshots or response snippets attached/linked if UI-facing

### 7. Support knowledge base

Produce a support article using [`SUPPORT_KB_TEMPLATE.md`](SUPPORT_KB_TEMPLATE.md). Minimum content for this story:

| KB section | Content for this story |
|------------|------------------------|
| Feature overview | One paragraph for support agents |
| Happy-path dataflow | Ingress → queue → pipelet(s) → destination → metrics/logs (adapt) |
| How to verify | API and/or UI checks |
| Failure modes | Common errors + what support should check (DLQ, 503, completeness) |
| Escalation metrics | Prom/Grafana/Kibana signals |

- [ ] Draft KB article reviewed (or placeholder linked under `docs/delivery/kb/`)
- [ ] Dataflow matches architecture for the feature

---

## Definition of Done (story)

- [ ] All AC checkboxes above complete
- [ ] Unit + integration tests run in CI (or Wave CI gate if CI not yet in place)
- [ ] No cross-tenant leakage demonstrated by tests where applicable
- [ ] `TEST_MATRIX.md` row updated
- `WAVE_TRACKER.md` status set to Done
- [ ] Architecture deviation (if any) noted in PR

---

## Notes / risks

-
