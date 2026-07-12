# KB: Level-1 nav shell + tenant context (Wave 6)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W6-US01 / W6-US01 |
| **Audience** | Frontend engineers / support |
| **Product area** | UI shell / tenancy |

## Prerequisites

- Node 20+ and npm
- `dashflow-ui` module on `wave-6` (or later)

## Feature overview

Vite + React + TypeScript app under `dashflow-ui/` with:

| Piece | Role |
|-------|------|
| `AuthContext` | Stub session (`isAuthenticated`, `displayName`); no IdP yet |
| `TenantContext` | Selected tenant id; persists to `sessionStorage` key `pipeline.tenantId` |
| `AppShell` | Level-1 nav per architecture §4.1 |
| `apiClient` | Stub `buildApiHeaders` / `apiFetch` that attach `X-Tenant-Id` |

### Primary nav map

| Label | Route |
|-------|--------|
| Pipelets | `/pipelets` |
| Pipelines | `/pipelines` (list); `/pipelines/new`, `/pipelines/:id` (builder) |
| Connectors | `/connectors` |
| Services | `/services` |
| Tenants | `/tenants` (list + register) |
| Billing | `/billing` |
| Observability | `/observability` |

Default route `/` redirects to `/pipelets`.

### Tenant picker

Header select loads from `GET /api/v1/tenants` (MSW seeds T001/T002). Register new tenants on **Tenants** → **New tenant** (`POST /api/v1/tenants`); create switches the active context automatically.

- `T001` — Acme Analytics (default)
- `T002` — Beta Logistics

Changing the picker updates `TenantContext` and `sessionStorage`. Later stories must read tenant id from context (never hard-code) when calling `/api/v1/*`.

## How to verify

```bash
cd dashflow-ui
npm install
npm test -- AuthContext.test Shell.test
npm run dev
```

Open the app, confirm nav links and tenant select; switch to `T002` and refresh — selection should stick for the tab session.

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Tests fail on `sessionStorage` | jsdom env | Vitest `environment: 'jsdom'` in `vite.config.ts` |
| Nav missing a §4.1 label | `src/app/navItems.ts` | Keep labels aligned with architecture |
| API calls without tenant | Call sites | Use `apiFetch(path, tenantId)` / `buildApiHeaders` |

## Related

- Developer TDD: [`../tdd/stories/w6/W6-US01-tdd.md`](../tdd/stories/w6/W6-US01-tdd.md)
- Architecture §4.1
- W1 `X-Tenant-Id` request context
