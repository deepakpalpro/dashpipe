# KB: Stage retries + DLQ (Wave 2)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W2-US06 / W2-US06 |
| **Audience** | Platform engineers / support |
| **Product area** | Messaging / Pipelines |

## Prerequisites

- W2-US03 platform broker topology (Wave 2 default: RabbitMQ)
- Compose RabbitMQ
- Architecture §5.1 (broker is pluggable; DLQ semantics are logical)

## Feature overview

Per-stage dead-letter path (architecture §8.1–§8.2). Logical DLQ name is broker-agnostic; Wave 2 implements it on RabbitMQ via DLX:

| Piece | Name |
|-------|------|
| DLX | `tenant.{tenantId}.pipeline.{pipelineId}.dlx` |
| Stage queue | `...stage.{n}.in` with `x-dead-letter-exchange` + RK `stage.{n}.dlq` |
| DLQ | `...stage.{n}.dlq` bound to DLX |

| Type | Class | Role |
|------|-------|------|
| Policy | `RetryPolicy` | Parses pipeline `retry_config` (`max_retries`, backoff) |
| Router | `StageDeadLetterService` | Republish while retries remain; else publish to DLX |

Headers on failure / DLQ messages:

- `x-pipeline-failure-count`
- `x-pipeline-error`

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashflow-api test -Dtest=RetryPolicyTest,StageDeadLetterServiceTest,StageDlqIT
```

## Support: “pipeline run failed”

1. Confirm execution status (`failed` / stuck `running`).
2. Check stage DLQ depth in RabbitMQ mgmt (`…stage.{n}.dlq`).
3. Inspect `x-pipeline-error` header on DLQ message.
4. Fix root cause; optional future: admin replay from DLQ → input queue.

## Related

- Developer TDD: [`../tdd/stories/w2/W2-US06-tdd.md`](../tdd/stories/w2/W2-US06-tdd.md)
- Execution status: [`W2-US07-execution-status.md`](W2-US07-execution-status.md)
- Async run: [`W2-US04-async-run.md`](W2-US04-async-run.md)
