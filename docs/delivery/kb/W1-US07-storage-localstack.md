# KB: Storage connector vs LocalStack S3 (Wave 1)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W1-US07 / W1-US07 |
| **Audience** | Platform engineers / juniors wiring S3 locally |
| **Product area** | Connectors / Storage |

## Prerequisites

- W1-US05 Connector SPI
- Compose LocalStack (`docker compose up -d localstack`)
- `./scripts/smoke-localstack.sh` exit 0
- Flyway through `V7__storage_connector_type.sql`

## Feature overview

`StorageConnector` (`type=storage`) uses AWS SDK v2 S3 client with:

| Setting | Default |
|---------|---------|
| Endpoint | `http://localhost:4567` (`LOCALSTACK_ENDPOINT`) |
| Region | `us-east-1` |
| Credentials | `test` / `test` |
| Access style | **path-style** (required for LocalStack) |

SPI mapping:

- `testConnection()` → ensure bucket + `headBucket`
- `write()` → `putObject` (key from `headers.key` or `recordId`)
- `read()` → `getObject`

Catalog: `GET /api/v1/connector-types` includes `storage` (`ct-storage`).

## Sample connector config

```json
{
  "bucket": "pp-tenant-demo",
  "endpoint": "http://localhost:4567",
  "region": "us-east-1",
  "createBucketIfMissing": true,
  "accessKeyId": "test",
  "secretAccessKey": "test"
}
```

Do not log secret keys.

## How to verify

```bash
docker compose up -d localstack
./scripts/smoke-localstack.sh
./mvnw -pl dashflow-api test -Dtest=StorageConnectorTest,StorageConnectorIT
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| IT skipped / assume fail | LocalStack down | `docker compose up -d localstack` |
| Hits real AWS | Missing `endpointOverride` | Always set `endpoint` for local |
| Signature / path errors | Path style off | Factory enables path-style |
| Port confusion | Using 4566 on host | Repo default host port is **4567** |

## Related

- Developer TDD: [`../tdd/stories/w1/W1-US07-tdd.md`](../tdd/stories/w1/W1-US07-tdd.md)
- LocalStack compose: [`W0-US01-local-compose-stack.md`](W0-US01-local-compose-stack.md)
- SPI: [`W1-US05-connector-spi.md`](W1-US05-connector-spi.md)
