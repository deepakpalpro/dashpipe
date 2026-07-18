# KB: Mock data + WireMock harness (Wave 0)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W0-US05 / W0-US05 |
| **Audience** | Platform engineers |
| **Product area** | Foundation / Test harness |

## Prerequisites

- Maven Wrapper (`./mvnw`)
- No Compose stack required for these unit-style harness tests

## Feature overview

Wave 0 provides:

1. **Tenant fixtures** under `dashpipe-api/src/test/resources/fixtures/tenants/` loaded via `TenantFixtures`
2. **WireMock harness** (`WireMockHarnessTest`) that stubs `GET /external/ping` for future Rest connector tests

## How to add a fixture

1. Add JSON under `dashpipe-api/src/test/resources/fixtures/<entity>/`.
2. Use deterministic IDs (e.g. `T001`) so later stories can reference them.
3. Load with classpath helpers (`TenantFixtures.load(...)`) — prefer classpath over filesystem paths so tests work from any working directory.
4. Assert the document in a `*Test` class.

Example tenant fixture: `fixtures/tenants/t001.json` → id `T001`, slug `demo`.

## How to extend WireMock

```java
@RegisterExtension
static final WireMockExtension WIRE_MOCK =
    WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

WIRE_MOCK.stubFor(
    get(urlEqualTo("/external/ping"))
        .willReturn(aResponse().withStatus(200).withBody("{\"ok\":true}")));
```

Point your client (or connector under test) at `WIRE_MOCK.baseUrl()`.

## How to verify

```bash
./mvnw -pl dashpipe-api test -Dtest=TenantFixturesTest,WireMockHarnessTest
```

Expect both classes green. Full module: `./mvnw -pl dashpipe-api test`.

## Related

- Health check: [`W0-US02-health-endpoint.md`](W0-US02-health-endpoint.md)
- Flyway baseline: [`W0-US03-flyway-baseline.md`](W0-US03-flyway-baseline.md)
