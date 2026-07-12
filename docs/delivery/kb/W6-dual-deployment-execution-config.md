# KB: Dual deployment & execution configuration

| Field | Value |
|-------|--------|
| **Article** | KB-W6-dual-config |
| **Audience** | Platform + frontend engineers |
| **Product area** | Pipelines / connectors / services / pipelets |
| **Last updated** | 2026-07-10 |

## Prerequisites

- Flyway through **V17** (`dual_deployment_execution_config`)
- Optional: V18 auth vendors, V19 local seed data

## Model

Every configurable entity carries **two** JSON maps:

| Kind | Purpose | Examples |
|------|---------|----------|
| **Deployment configuration** | Where / how the workload is placed | `cloud`, `region`, `replicas`, `accessKey` |
| **Execution configuration** | Runtime behaviour | `batchSize`, `mapping`, `baseUrl`, `timeoutMs` |

**Defaults** live on the catalog (pipelet fixture, `connector_types`, `service_defaults`).  
**Instances** (pipeline, step, connector, service) may **override** existing keys and **extend** with new keys.

Merge rule (shallow):

```text
effective = { ...defaults, ...overrides }
```

Secret-looking keys sent as `***` / `••••••` on update keep the stored value (`DualConfigSupport.mergePreservingSecrets`).

### Legacy aliases

| Surface | Legacy field | Alias of |
|---------|--------------|----------|
| Pipeline step | `config` | `execution_config` |
| Connector | `config` | `execution_config` |
| Service | `tenant_config` | `execution_config` |

APIs accept either; responses expose both where applicable.

## Schema (Flyway)

| Migration | Change |
|-----------|--------|
| V16 | `pipelines.deployment_config` |
| V17 | `execution_config` on pipelines; dual columns on `pipeline_steps`, `connectors`, `services`; defaults on `service_defaults` / `connector_types` |
| V18 | Auth vendor rows under `st-auth` (OAuth, OIDC, Keycloak, AAD, AWSCognito, AzureMI, CertBased, JWT) |
| V19 | Dev seed: T001/T002, one connector per pipelet for T001, Auth service instances |

## UI

| Surface | Behaviour |
|---------|-----------|
| Pipeline builder | Pipeline-level + step-level dual KeyValue editors |
| Connectors / Services | Dual editors on create and edit |
| Pipelet fixture | `deploymentConfiguration` + `executionConfiguration` defaults (~105 entries) |
| Connectors list | Search, type/status filter, pagination (20/page) |
| Builder binding | Searchable connector/service dropdown |

## How to verify

```bash
# API migrations
./mvnw -pl dashflow-api flyway:info \
  -Dflyway.url='jdbc:mysql://localhost:3306/pipeline?allowPublicKeyRetrieval=true&useSSL=false' \
  -Dflyway.user=pipeline -Dflyway.password=pipeline

# UI
cd dashflow-ui && npm test -- --run
```

Live UI: `npm run dev:api` against `dashflow-api` (`local` profile).

## Related

- [`SERVICE_CONNECTOR_PIPELET_MODEL.md`](../../SERVICE_CONNECTOR_PIPELET_MODEL.md)
- [`W6-US02-connectors-services-ui.md`](W6-US02-connectors-services-ui.md)
- [`W6-US04-pipeline-builder.md`](W6-US04-pipeline-builder.md)
- [`W1-US03-service-types.md`](W1-US03-service-types.md)
