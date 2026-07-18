# KB: MessageBus connector vs LocalStack SQS (Wave 1)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W1-US08 / W1-US08 |
| **Audience** | Platform engineers |
| **Product area** | Connectors / MessageBus |
| **Priority** | Should (completed) |

## Prerequisites

- Compose LocalStack with SQS enabled
- `./scripts/smoke-localstack.sh` exit 0
- Flyway through `V8__message_bus_connector_type.sql`

## Feature overview

`MessageBusConnector` (`type=message_bus`) publishes/receives via **AWS SQS API against LocalStack**.

This is **not** the platform’s internal RabbitMQ topology (Wave 2 inter-stage queues). Use this connector when a pipeline step talks to an **external** SQS-compatible bus.

| Setting | Default |
|---------|---------|
| Endpoint | `http://localhost:4567` |
| Region | `us-east-1` |
| Credentials | `test` / `test` |

SPI:

- `testConnection()` → ensure queue + `getQueueAttributes`
- `write()` → `sendMessage`
- `read()` → `receiveMessage` (long-poll 1s)

Queue URLs returned by LocalStack (`*.localhost.localstack.cloud:4566`) are rewritten to the configured host endpoint (port **4567**).

## Sample config

```json
{
  "queueName": "pp-tenant-events",
  "endpoint": "http://localhost:4567",
  "region": "us-east-1",
  "createQueueIfMissing": true
}
```

## How to verify

```bash
docker compose up -d localstack
./scripts/smoke-localstack.sh
./mvnw -pl dashpipe-api test -Dtest=MessageBusConnectorTest,MessageBusConnectorIT
```

## Related

- Developer TDD: [`../tdd/stories/w1/W1-US08-tdd.md`](../tdd/stories/w1/W1-US08-tdd.md)
- Storage LocalStack: [`W1-US07-storage-localstack.md`](W1-US07-storage-localstack.md)
- Compose: [`W0-US01-local-compose-stack.md`](W0-US01-local-compose-stack.md)
