# KB: Execution status query API (Wave 2)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W2-US07 / W2-US07 |
| **Audience** | Platform engineers / tenant operators |
| **Product area** | Pipelines / Executions |

## Prerequisites

- W2-US04 async run (`pipeline_executions` + `POST .../run`)
- Compose MySQL + RabbitMQ
- Stub auth: `X-Tenant-Id`

## Feature overview

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/api/v1/pipelines/{id}/executions` | List for pipeline; `started_at` desc |
| `GET` | `/api/v1/pipelines/{id}/executions/{executionId}` | Detail |

Response fields include `id`, `pipeline_id`, `status`, `started_at`, `completed_at`, counts. Per-stage metrics dashboards are Wave 4.

Tenant isolation: foreign pipeline id → **404** (list and get).

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashflow-api test -Dtest=ExecutionStatusIT,PipelineRunIT
```

## Manual curl sketch

```bash
# after POST .../run → EXEC_ID
curl -s "localhost:8080/api/v1/pipelines/$PIPE_ID/executions" \
  -H "X-Tenant-Id: $TENANT_ID"

curl -s "localhost:8080/api/v1/pipelines/$PIPE_ID/executions/$EXEC_ID" \
  -H "X-Tenant-Id: $TENANT_ID"

# other tenant → 404
curl -s -o /dev/null -w "%{http_code}\n" \
  "localhost:8080/api/v1/pipelines/$PIPE_ID/executions" \
  -H "X-Tenant-Id: $OTHER_TENANT_ID"
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 404 on list/get | Wrong tenant or pipeline id | Matching `X-Tenant-Id` |
| Empty list | No runs yet | `POST .../run` first |
| Status stuck `running` | Worker / RabbitMQ | See W2-US04 KB |

## Related

- Developer TDD: [`../tdd/stories/w2/W2-US07-tdd.md`](../tdd/stories/w2/W2-US07-tdd.md)
- Async run: [`W2-US04-async-run.md`](W2-US04-async-run.md)
- Wave exit: [`../waves/WAVE_2.md`](../waves/WAVE_2.md) Definition of Done
