# KB: Async pipeline run (Wave 2)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W2-US04 / W2-US04 |
| **Audience** | Platform engineers / tenant operators |
| **Product area** | Pipelines / Execution |

## Prerequisites

- W2-US01–US03 (CRUD, steps, RabbitMQ topology)
- Compose MySQL + RabbitMQ
- Flyway through `V11__pipeline_executions.sql`
- Pipeline must be **`active`** with at least one step

## Feature overview

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/api/v1/pipelines/{id}/run` | **202** + `execution_id`; starts async handoff |
| `GET` | `/api/v1/pipelines/{id}/executions/{executionId}` | Minimal status read (deepened in W2-US07) |

Flow:

1. Persist `pipeline_executions` (`pending` → `running`)
2. Declare tenant-prefixed topology
3. Publish `StageMessage` to stage-1 exchange **and** stub worker queue
4. `StubStageWorker` advances stages until last → `completed`

Real K8s Jobs replace the stub in **W2-US05**.

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashflow-api test -Dtest=PipelineRunOrchestratorTest,PipelineRunIT
```

## Manual curl sketch

```bash
# activate pipeline first (PUT status=active), then:
curl -s -X POST "localhost:8080/api/v1/pipelines/$PIPE_ID/run" \
  -H "X-Tenant-Id: $TENANT_ID"
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 400 draft/archived | Status not `active` | PUT status to `active` |
| 400 no steps | Empty steps | `PUT .../steps` first |
| 404 on run | Wrong tenant | Matching `X-Tenant-Id` |
| Stuck `running` | RabbitMQ down / listener | `docker compose up -d rabbitmq` |

## Related

- Developer TDD: [`../tdd/stories/w2/W2-US04-tdd.md`](../tdd/stories/w2/W2-US04-tdd.md)
- Next: pipelet Job client (W2-US05)
