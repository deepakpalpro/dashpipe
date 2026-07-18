# KB: Connector SPI + Rest plugin (Wave 1)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W1-US05 / W1-US05 |
| **Audience** | Platform engineers / junior plugin authors |
| **Product area** | Connectors / SPI |

## Prerequisites

- Wave 0 WireMock harness patterns (for US06)
- Compose MySQL; Flyway through `V5__connector_types.sql`

## Feature overview

Connectors implement `com.dashflow.connector.spi.Connector` (architecture §9.1). Wave 1 registers plugins as **Spring `@Component` beans** collected by `ConnectorRegistry` — **not** PF4J JAR scan yet (deferred; document when adding plugins).

Built-in first plugin: **`RestConnector`** (`type=rest`, `spiVersion=1.0`).

Catalog API: `GET /api/v1/connector-types` lists seeded rows (includes `rest`).

`testConnection()` validates config presence (`baseUrl`); real HTTP probe is **W1-US06**.

## How to add a connector plugin (checklist)

1. Implement `Connector` in `com.dashflow.connector.<type>`.
2. Annotate with `@Component` (or register a `@Bean`).
3. Return a unique `getType()` string matching architecture §9.5.
4. Add Flyway seed row to `connector_types` (`spi_class`, `spi_version`, `config_schema`).
5. Unit-test `getType` + `testConnection` failure without config.
6. Extend `ConnectorSpiLoaderTest` (or add type-specific loader assert).

Do **not** invent a second SPI — extend §9.

## How to verify

```bash
docker compose up -d mysql
./mvnw -pl dashflow-api test -Dtest=ConnectorSpiLoaderTest,RestConnectorTest,ConnectorTypeControllerIT
```

Boot logs should include: `Registered connector plugin type=rest ...`

```bash
curl -s localhost:8080/api/v1/connector-types
# expect type=rest, spiClass=...RestConnector
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Registry missing `rest` | Component scan limited to `api` package | `scanBasePackages = "com.dashflow"` |
| Duplicate type boot failure | Two beans same `getType()` | One implementation per type |
| Loader test can't find Boot config | Test outside `api` package | `@SpringBootTest(classes = PipelineApiApplication.class)` |

## Related

- Developer TDD: [`../tdd/stories/w1/W1-US05-tdd.md`](../tdd/stories/w1/W1-US05-tdd.md)
- Next: [`../tdd/stories/w1/W1-US06-tdd.md`](../tdd/stories/w1/W1-US06-tdd.md) (WireMock HTTP test)
- Architecture §9
- Modeling (connector vs step; ADLS example): [`../../SERVICE_CONNECTOR_PIPELET_MODEL.md`](../../SERVICE_CONNECTOR_PIPELET_MODEL.md)
