# KB: Health endpoint (Wave 0)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W0-US02 / W0-US02 |
| **Audience** | Platform engineers / support |
| **Product area** | Foundation / API |

## Prerequisites

- Wave 0 `dashflow-api` module
- MySQL available (Compose or Testcontainers in tests)

## Feature overview

The platform API exposes Spring Boot Actuator health so operators and CI can confirm the process is up and can reach MySQL. No business APIs are required for this check.

## Happy-path dataflow

```mermaid
flowchart LR
  Client[curl_or_probe] --> Actuator["/actuator/health"]
  Actuator --> App[SpringBoot]
  App --> MySQL[(MySQL)]
```

## How to verify

### API

```bash
curl -s http://localhost:8080/actuator/health
```

Expect HTTP 200 and `"status":"UP"`.

### Tests

```bash
docker compose up -d mysql
./mvnw -pl dashflow-api test
```

`HealthControllerIT` uses the Compose `local` profile (MySQL on `localhost:3306`). It skips if port 3306 is closed. Testcontainers MySQL is deferred on Rancher Desktop until docker-java supports Engine API ≥ 1.41.

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Connection refused | App not started | Start with `local` profile |
| Status DOWN / db | MySQL URL/user/password | Align `application-local.yml` with Compose |
| IT flaky | Docker for Testcontainers | Enable Docker; increase startup timeout |

## Related

- Flyway baseline (W0-US03): tables appear after successful migrate on same datasource
- Compose stack: [`W0-US01-local-compose-stack.md`](W0-US01-local-compose-stack.md)
