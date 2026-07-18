# KB: Service types + platform defaults (Wave 1)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W1-US03 / W1-US03 |
| **Audience** | Platform engineers |
| **Product area** | Services / Catalog |

## Prerequisites

- Compose MySQL; Flyway through `V3__service_types.sql`
- App `local` profile

## Feature overview

**Service types** are a **global** platform catalog (not tenant-scoped). Each type (e.g. `auth`) can have one or more **vendors** with:

- `default_config` — non-secret platform defaults tenants may inherit (W1-US04)
- `config_schema` — JSON Schema describing allowed fields (including secret fields marked writeOnly)

Wave 1 seeds:

| ID | Type | Vendor |
|----|------|--------|
| `st-auth` | `auth` | — |
| `sd-auth-stub` | — | `StubAuth` |

**Wave 6 follow-up (Flyway V18)** adds Auth vendor defaults under `st-auth`:

| ID | Vendor |
|----|--------|
| `sd-auth-oauth` | OAuth |
| `sd-auth-oidc` | OIDC |
| `sd-auth-keycloak` | Keycloak |
| `sd-auth-aad` | AAD |
| `sd-auth-cognito` | AWSCognito |
| `sd-auth-azure-mi` | AzureMI |
| `sd-auth-cert` | CertBased |
| `sd-auth-jwt` | JWT |

Tenant-specific Auth config (secrets, overrides) is **W1-US04**. Dual deployment/execution maps: [`W6-dual-deployment-execution-config.md`](W6-dual-deployment-execution-config.md).

## How to verify

```bash
./mvnw -pl dashpipe-api spring-boot:run -Dspring-boot.run.profiles=local
curl -s http://localhost:8080/api/v1/service-types | jq .
```

Expect `type: "auth"`, vendor `StubAuth`, and a `defaultConfig.issuer`.

### Tests

```bash
./mvnw -pl dashpipe-api test -Dtest=ServiceTypeServiceTest,ServiceTypeControllerIT
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Empty catalog | Flyway not at v3 | Restart app; inspect `flyway_schema_history` |
| Duplicate key on migrate | Re-seed conflict | Do not re-run INSERT outside Flyway |
| Expecting tenant header | Catalog is global | No `X-Tenant-Id` required |

## Related

- Developer TDD: [`../tdd/stories/w1/W1-US03-tdd.md`](../tdd/stories/w1/W1-US03-tdd.md)
- Next: [`../tdd/stories/w1/W1-US04-tdd.md`](../tdd/stories/w1/W1-US04-tdd.md)
- Architecture §2 `service_types` / `service_defaults`, §3.4
