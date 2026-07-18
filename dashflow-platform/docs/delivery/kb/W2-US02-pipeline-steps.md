# KB: Pipeline steps config API (Wave 2)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W2-US02 / W2-US02 |
| **Audience** | Platform engineers / tenant admins |
| **Product area** | Pipelines |

## Prerequisites

- W2-US01 pipeline CRUD
- Compose MySQL; Flyway through `V10__pipeline_steps.sql`
- Stub tenancy: `X-Tenant-Id`

## Feature overview

### What a step is (important)

A **pipeline step** is the per-pipeline **configuration of a pipelet** that will run as an ephemeral Job/Pod when the pipeline executes (architecture §2 `pipeline_steps`, §10.3).

| Concept | Role |
|---------|------|
| **Pipelet** | Reusable unit (image + `config_schema`) — *what can run* |
| **Pipeline step** | Binding into *this* pipeline — *how it runs here* |
| **Job / Pod** | Runtime for one step of one execution — *the actual run* |

Step fields map to the future pod:

| Step field | Used for |
|------------|----------|
| `pipelet_id` | Which pipelet image/type to spawn |
| `step_order` | Stage sequence → Job name `…-stage-{n}` |
| `config` | Pipelet-specific settings (vs pipelet `config_schema`) |
| `connector_ids` / `service_ids` | Credentials/services injected into the pod |
| `input_queue` / `output_queue` | Logical stage destinations on the **platform message broker** (Wave 2: RabbitMQ; pluggable per architecture §5.1) |
| `resource_limits` | CPU/memory limits on the Job |

### API

Full **replace** of a pipeline’s step sequence:

| Method | Path | Notes |
|--------|------|-------|
| `PUT` | `/api/v1/pipelines/{id}/steps` | Delete existing steps, insert new set; bumps pipeline `version` |
| `GET` | `/api/v1/pipelines/{id}` | Returns ordered `steps` |

**Empty `steps` array is rejected** (`@NotEmpty`) so a 3-stage fixture can be required later. Queue names may be placeholders until W2-US03 declares RabbitMQ. Pipelet registry FK is deferred in Wave 2 — `pipelet_id` is an opaque string until the pipelet catalog story lands.

Isolation: filtered pipeline lookup — cross-tenant PUT → **404**.

## How to verify

```bash
docker compose up -d mysql
./mvnw -pl dashflow-api test -Dtest=PipelineStepsServiceTest,PipelineStepsIT
```

## Manual curl sketch

```bash
curl -s -X PUT "localhost:8080/api/v1/pipelines/$PIPE_ID/steps" \
  -H 'Content-Type: application/json' -H "X-Tenant-Id: $TENANT_ID" \
  -d '{"steps":[{"pipelet_id":"plet-rest-source","step_order":1,"config":{"batch_size":100},"connector_ids":["conn-1"],"service_ids":[],"input_queue":"t.demo.s1.in","output_queue":"t.demo.s1.out"}]}'
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 404 on PUT | Wrong tenant or pipeline id | Use owning tenant’s `X-Tenant-Id` |
| 400 empty steps | `@NotEmpty` on `steps` | Send at least one step |
| 400 duplicate order | Unique `(pipeline_id, step_order)` | Distinct `step_order` values |

## Related

- Developer TDD: [`../tdd/stories/w2/W2-US02-tdd.md`](../tdd/stories/w2/W2-US02-tdd.md)
- Execution plan: [`../waves/WAVE_2.md`](../waves/WAVE_2.md) § Core model
- Job spawn from steps: [`W2-US05-pipelet-job.md`](W2-US05-pipelet-job.md)
- Architecture: [`../../../ARCHITECTURE.md`](../../../ARCHITECTURE.md) §2 `pipeline_steps`, §5.1 pluggable broker, §10.3
- Next: platform broker topology (W2-US03; RabbitMQ default)
