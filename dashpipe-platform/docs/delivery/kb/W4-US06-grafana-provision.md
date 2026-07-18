# KB: Grafana provision (Wave 4)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W4-US06 / W4-US06 |
| **Audience** | Platform engineers / SRE |
| **Product area** | Observability / Grafana |

## Prerequisites

- W4-US02 completeness metrics (dashboard panels reference them)
- Architecture §7.2 (one Grafana; org per tenant)

## Mental model (read this first)

**One Grafana. Many tenant orgs.**

| Myth | Reality |
|------|---------|
| “We provision Grafana for each tenant” means a new Grafana server | **False.** There is a **single** Grafana instance in platform infra |
| Each tenant gets isolation by logging into that Grafana | **True** — via a dedicated **Grafana Organization**, not by filtering one shared org’s dashboards |
| Prometheus is also per-tenant | **False.** One Prometheus; series labeled with `tenant_id` |

```text
┌─────────────────────────────────────────┐
│  Grafana (one instance)                 │
│  ┌─────────────┐  ┌─────────────┐       │
│  │ Org Tenant A│  │ Org Tenant B│  ...  │
│  │ dashboards  │  │ dashboards  │       │
│  │ users/alerts│  │ users/alerts│       │
│  └─────────────┘  └─────────────┘       │
└─────────────────────────────────────────┘
              ▲
              │ scrape / query
        Prometheus (one)
        metrics{tenant_id=...}
```

**Why org-per-tenant?** Organizations are a hard boundary (users, dashboards, datasources, alert rules). A single org with folder ACLs or a `tenant_id` dashboard variable is fine for **internal support**, but weaker if **tenant customers** log in. This product defaults to org-per-tenant for tenant-facing isolation (same idea as Kibana **space** per tenant on one ELK stack).

**Alternatives (not Wave 4 default):** one org + folders/Teams; or one dashboard + `tenant_id` variable for ops-only.

## Feature overview

Wave 4 ships a **stub** provision path (no live Grafana required in CI):

| Component | Role |
|-----------|------|
| `GrafanaClient` | Interface (`createOrg`, `upsertDashboard`) |
| `StubGrafanaClient` | In-memory recording client |
| `GrafanaProvisioner` | Maps tenant → **org** + template dashboard (not a new Grafana install) |
| Template | `classpath:grafana/tenant-pipeline-overview.json` |
| Optional API | `POST /api/v1/tenants/{id}/grafana` → `201` |

**Stub vs real:** Replace `StubGrafanaClient` with an HTTP client against the **shared** Grafana Admin API when ops Grafana is available. Tokens stay in env/config — never commit secrets.

### Local Compose Grafana + Prometheus

CI still uses the stub. For a real local stack (optional):

```bash
docker compose --profile metrics up -d
./scripts/smoke-metrics.sh
```

| Service | URL | Notes |
|---------|-----|-------|
| Prometheus | http://localhost:9090 | Scrapes `host.docker.internal:8080/actuator/prometheus` |
| Grafana | http://localhost:3000 | `admin` / `admin`; Prometheus datasource + **Pipeline Overview (local)** dashboard provisioned |

Point the UI “open Grafana” button at the local instance:

```bash
export PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL=http://localhost:3000
# optional ES: PIPELINE_OBSERVABILITY_ELASTICSEARCH_BASE_URL=http://localhost:9200
./mvnw -pl dashpipe-api spring-boot:run -Dspring-boot.run.profiles=local
```

Provisioning files live under `deploy/prometheus/` and `deploy/grafana/`. Tenant **org** provision via `POST /api/v1/tenants/{id}/grafana` still hits the stub unless a live `GrafanaClient` is wired.

## How to verify

```bash
./mvnw -pl dashpipe-api test -Dtest=GrafanaProvisionerTest
```

Manual (with API running):

```bash
curl -s -X POST "http://localhost:8080/api/v1/tenants/<tenant-id>/grafana"
```

Expect a provision result with `orgId` / `orgName` / `dashboardUid` — that is an org **inside** the shared Grafana, not a new host.

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 404 on provision | Unknown tenant id | Create tenant first |
| Missing template | Classpath resource | Confirm `grafana/tenant-pipeline-overview.json` |
| Expecting live Grafana | Stub only in Wave 4 CI | Use Compose `metrics` profile for local UI; wire real client later |
| Expecting N Grafana servers | Misread “provision” | One instance; N orgs |
| Prometheus target down | API not on host `:8080`, or bad `host.docker.internal` | Start API on `:8080`; on Rancher Desktop do **not** pin `host.docker.internal` to `host-gateway` (see `docker-compose.yml`); recreate Prometheus; check http://localhost:9090/targets |

## Related

- Developer TDD: [`../tdd/stories/w4/W4-US06-tdd.md`](../tdd/stories/w4/W4-US06-tdd.md)
- Completeness: [`W4-US02-completeness.md`](W4-US02-completeness.md)
- Architecture §7.2
- Modeling (completeness + pipelet telemetry in Grafana): [`../../SERVICE_CONNECTOR_PIPELET_MODEL.md`](../../SERVICE_CONNECTOR_PIPELET_MODEL.md) §5
