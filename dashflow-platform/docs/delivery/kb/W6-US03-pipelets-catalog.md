# KB: Pipelets catalog + admin register (Wave 6)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W6-US03 / W6-US03 |
| **Audience** | Frontend engineers / support |
| **Product area** | UI pipelets catalog |

## Prerequisites

- W6-US01 shell
- Optional MSW (`VITE_ENABLE_MSW=true`) for register stub

## Feature overview

`/pipelets` shows a global catalog of Source / Processor / Destination cards with search and category tabs (~105 fixture entries).

| Piece | Role |
|-------|------|
| `src/fixtures/pipelets.json` | Static catalog (opaque W2-style ids) with `deploymentConfiguration` + `executionConfiguration` defaults |
| `catalogFilter` | Pure category + text filter |
| `RegisterPipeletModal` | Admin tabs: Image Path / Image URL / Runtime Binary |
| `roleGate` / `AuthContext.isAdmin` | Register button for Operator/Admin stub names |
| Builder palette | Same fixture; grouped categories with search + “Show more” |

### Fixture → live API swap

Today the page loads `PIPELET_FIXTURE` in-process. MSW also serves:

- `GET /api/v1/pipelets` → fixture JSON
- `POST /api/v1/pipelets/register` → `201` stub

To switch to a live registry: replace `catalog = PIPELET_FIXTURE` with `listPipelets(tenantId)` (same shape) and keep the fixture for unit tests.

### Admin stub

Default session display name `Platform Operator` → `isAdmin === true`. Tenant-only browse: sign in stub without “admin”/“operator” in the name.

## How to verify

```bash
cd dashflow-ui
npm test -- catalogFilter PipeletsCatalog RegisterPipeletModal
```

Manual: open Pipelets → filter Source → search “REST”; click Register → fill name + image path.

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| No Register button | `isAdmin` / display name | Include Operator or Admin in stub name |
| Filter wrong count | `catalogFilter` unit tests | Keep filter pure |
| Register 404 without MSW | Live API missing | Enable MSW or noop `onRegister` in tests |

## Related

- Developer TDD: [`../tdd/stories/w6/W6-US03-tdd.md`](../tdd/stories/w6/W6-US03-tdd.md)
- Architecture §4.2
