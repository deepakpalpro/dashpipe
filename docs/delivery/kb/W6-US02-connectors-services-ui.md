# KB: Connectors & Services UI (Wave 6)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W6-US02 / W6-US02 |
| **Audience** | Frontend engineers / support |
| **Product area** | UI catalogs / connectors / services |
| **Last updated** | 2026-07-10 |

## Prerequisites

- W6-US01 shell (`dashflow-ui`)
- Node 20+ / npm
- Optional: MSW (`npm run dev`) or live API (`npm run dev:api`)
- Live API: Flyway schema + **V19 seed** when Spring profile `local` is on (`classpath:db/seed`); Azure empty stack skips seed

## Feature overview

| Surface | Behaviour |
|---------|-----------|
| `/connectors` | Split view with **search**, **type/status filters**, **pagination** (20/page); detail/create on the right |
| `/services` | Table list + create form; click name for detail; vendor dropdown from `service-types.defaults` |
| MSW | `src/mocks/handlers.ts` mirrors W1 `/api/v1/connectors` and `/api/v1/services` (IDs aligned to Flyway: `ct-rest`, `st-auth`) |
| Secrets | GET responses redact keys (`***`); UI displays `••••••` via `displayConfigValue` |

### Dual configuration

Create/edit forms expose two KeyValue editors:

- **Deployment configuration** → `deployment_config`
- **Execution configuration** → `execution_config` (also written to legacy `config` / `tenantConfig`)

See [`W6-dual-deployment-execution-config.md`](W6-dual-deployment-execution-config.md).

Create payloads (live API):

```json
{
  "connectorTypeId": "ct-rest",
  "name": "Orders API",
  "config": { "baseUrl": "https://…", "api_key": "…" },
  "deployment_config": { "cloud": "aws", "region": "us-east-1" },
  "execution_config": { "baseUrl": "https://…", "api_key": "…" }
}
```

```json
{
  "serviceTypeId": "st-auth",
  "vendor": "OAuth",
  "name": "OAuth Auth",
  "tenantConfig": { "client_id": "…", "client_secret": "…" },
  "deployment_config": { "cloud": "aws", "region": "us-east-1" },
  "execution_config": { "client_id": "…", "client_secret": "…" }
}
```

All fetches go through `apiFetch` with `X-Tenant-Id` from `TenantContext`.

### Local MSW

```bash
cd dashflow-ui
npm run dev          # MSW on
npm run dev:api      # live API via Vite /api proxy
```

### Fixtures / seed

| Layer | Content |
|-------|---------|
| MSW | `conn-1` Orders API + `svc-1` Primary Auth (StubAuth) **plus** `fixtures/seed-connectors.json` (~105) and `fixtures/seed-auth-services.json` (8 Auth vendors) |
| Flyway V18 | Auth vendor defaults: OAuth, OIDC, Keycloak, AAD, AWSCognito, AzureMI, CertBased, JWT (+ StubAuth from V3) |
| Flyway V19 | T001/T002 tenants; T001 one connector per pipelet; Auth service instances for both tenants |

## How to verify

```bash
cd dashflow-ui
npm test -- ConnectorForm ServiceForm ConnectorsList ServiceDetail connectorListFilter SearchableSelect
```

Manual: Connectors → search `webhook` → filter type Event Listener → open detail; Services → confirm OAuth / Keycloak / AAD rows (live) or Primary Auth (MSW).

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Empty lists in tests | MSW server in `src/test/setup.ts` | `server.listen` + `resetMockDb` |
| Secret visible in DOM | `ServiceDetail` / `displayConfigValue` | Never render raw GET secret fields |
| 400 on create | Client validation vs API | Required: type, name, baseUrl / vendor |
| Vendor rejected | Vendor not in `service_defaults` | Apply Flyway V18; pick from dropdown |
| Only a few connectors live | V19 not applied | Restart API / `flyway:migrate` |

## Related

- Developer TDD: [`../tdd/stories/w6/W6-US02-tdd.md`](../tdd/stories/w6/W6-US02-tdd.md)
- Dual config: [`W6-dual-deployment-execution-config.md`](W6-dual-deployment-execution-config.md)
- W1 SecretRedactor · Architecture §4.5 / §4.6
- Shell KB: [`W6-US01-nav-shell.md`](W6-US01-nav-shell.md)
