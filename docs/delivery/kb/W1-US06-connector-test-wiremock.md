# KB: Rest connector test vs WireMock (Wave 1)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W1-US06 / W1-US06 |
| **Audience** | Platform engineers / juniors debugging connector test |
| **Product area** | Connectors / Rest |

## Prerequisites

- W1-US05 Connector SPI + Rest plugin
- Compose MySQL; Flyway through `V6__connectors.sql`
- Stub tenant header `X-Tenant-Id`

## Feature overview

Tenant Rest connectors are stored in `connectors` and tested with:

```text
POST /api/v1/connectors/{id}/test
```

`RestConnector` GETs `{baseUrl}{pingPath}` (default ping path `/external/ping`, same as W0 WireMock harness). Success response shape (architecture §3.3):

```json
{
  "success": true,
  "latency_ms": 12,
  "message": "Connection successful",
  "tested_at": "..."
}
```

Cross-tenant test → **404**. HTTP 5xx from the target → `success: false` (API still 200 with body).

## Point a Rest connector at WireMock locally

1. Run WireMock (or use the JUnit `WireMockExtension` in tests).
2. Stub `GET /external/ping` → `200 {"ok":true}`.
3. Create connector:

```bash
curl -s -X POST localhost:8080/api/v1/connectors \
  -H 'Content-Type: application/json' -H "X-Tenant-Id: $TENANT_ID" \
  -d "{\"connectorTypeId\":\"ct-rest\",\"name\":\"wm\",\"config\":{\"baseUrl\":\"http://localhost:WIREMOCK_PORT\",\"pingPath\":\"/external/ping\"}}"
```

4. Test:

```bash
curl -s -X POST localhost:8080/api/v1/connectors/$CONN_ID/test \
  -H "X-Tenant-Id: $TENANT_ID"
```

## How to verify (automated)

```bash
docker compose up -d mysql
./mvnw -pl dashflow-api test -Dtest=RestConnectorTest,RestConnectorTestIT
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| `success: false` / connection refused | `baseUrl` wrong / WireMock down | Use `WIRE_MOCK.baseUrl()` in tests |
| Cross-tenant 200 | Filter not applied | Use filtered JPQL + `X-Tenant-Id` |
| No request on WireMock | Wrong `pingPath` | Default `/external/ping` |

## Related

- Developer TDD: [`../tdd/stories/w1/W1-US06-tdd.md`](../tdd/stories/w1/W1-US06-tdd.md)
- SPI: [`W1-US05-connector-spi.md`](W1-US05-connector-spi.md)
- W0 WireMock: [`W0-US05-mock-data-wiremock.md`](W0-US05-mock-data-wiremock.md)
