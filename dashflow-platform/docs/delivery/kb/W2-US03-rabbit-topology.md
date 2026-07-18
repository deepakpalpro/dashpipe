# KB: Inter-stage message broker topology (Wave 2)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W2-US03 / W2-US03 |
| **Audience** | Platform engineers |
| **Product area** | Messaging / Pipelines |

## Prerequisites

- Compose RabbitMQ (`docker compose up -d rabbitmq`) — AMQP `5672`, mgmt `15672` (`pipeline`/`pipeline`) — **Wave 2 default broker**
- Spring AMQP on `dashflow-api` (`spring-boot-starter-amqp`)
- Local profile RabbitMQ settings in `application.yml`

## Pluggable platform broker

The platform’s inter-stage bus is **configurable** (architecture §5.1). Supported *targets* include RabbitMQ, Kafka, SQS, Azure Event Hubs, ActiveMQ, and similar technologies.

| Concern | Wave 2 | Later |
|---------|--------|-------|
| Platform stage handoff / DLQ / ingress | **RabbitMQ** (this story) | Broker SPI adapters |
| Tenant external bus | `message_bus` connector (SQS/LocalStack) | Other connector plugins |

Logical names (`QueueNaming`, step `input_queue` / `output_queue`) stay the same; adapters map them to each product’s primitives.

## Feature overview (RabbitMQ default)

Tenant-prefixed stage topology (architecture appendix / §6.1 / §8.2):

```
Exchange: tenant.{tenantId}.pipeline.{pipelineId}   (topic)
Queue:    tenant.{tenantId}.pipeline.{pipelineId}.stage.{n}.in
DLQ:      tenant.{tenantId}.pipeline.{pipelineId}.stage.{n}.dlq
RK:       stage.{n}
```

| Type | Class | Role |
|------|-------|------|
| Naming | `QueueNaming` | Shared logical builder (also webhook helpers for W3) |
| Declare | `PipelineTopologyService` | Idempotent exchange/queue/binding via `AmqpAdmin` (RabbitMQ adapter) |

When steps are saved without queue fields, `PipelineStepsService` fills `input_queue` / `output_queue` from `QueueNaming` (last stage has no platform output).

**Out of scope here:** dead-letter *routing* policy (W2-US06); run orchestration (W2-US04); webhook declare (W3); non-RabbitMQ adapters.

## How to verify

```bash
docker compose up -d mysql rabbitmq
./mvnw -pl dashflow-api test -Dtest=QueueNamingTest,PipelineTopologyServiceTest,RabbitTopologyIT
```

## Manual sketch

```bash
# After app start with local profile, declare via service / future admin API.
# Mgmt UI: http://localhost:15672  (pipeline / pipeline)
# Look for exchanges matching tenant.<id>.pipeline.<id>
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| IT skipped | Port 5672 closed | `docker compose up -d rabbitmq` |
| Context fails on RabbitAdmin | Inject `AmqpAdmin` | Boot auto-configures `AmqpAdmin` |
| Cross-tenant collision | Missing tenant prefix | Always use `QueueNaming` |

## Related

- Architecture §5.1 pluggable broker
- Developer TDD: [`../tdd/stories/w2/W2-US03-tdd.md`](../tdd/stories/w2/W2-US03-tdd.md)
- Next: async run (W2-US04)
