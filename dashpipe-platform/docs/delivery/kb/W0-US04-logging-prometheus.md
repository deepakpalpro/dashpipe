# KB: Structured logging + Prometheus (Wave 0)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W0-US04 / W0-US04 |
| **Audience** | Platform operators / engineers |
| **Product area** | Foundation / Observability |

## Prerequisites

- Compose MySQL running (`docker compose up -d mysql`)
- `dashpipe-api` with `local` profile

## Feature overview

Wave 0 exposes:

1. **Prometheus scrape** at `/actuator/prometheus` (Micrometer JVM + process metrics)
2. **Structured console logs** via Spring Boot Logstash JSON format (`logging.structured.format.console=logstash`)
3. Explicit `logback-spring.xml` entry point for later ELK wiring (Wave 4)

Do not log secrets (DB passwords, tokens) in message text or MDC.

## How to verify

### Prometheus scrape

```bash
./mvnw -pl dashpipe-api spring-boot:run -Dspring-boot.run.profiles=local
curl -s http://localhost:8080/actuator/prometheus | head
```

Expect text exposition format including `jvm_memory_used_bytes`.

Local scrape URL: `http://localhost:8080/actuator/prometheus`

Optional local Prometheus + Grafana (Compose profile `metrics`):

```bash
docker compose --profile metrics up -d
./scripts/smoke-metrics.sh
```

Prometheus scrapes the host API; Grafana is at http://localhost:3000 (`admin` / `admin`). See [`W0-US01-local-compose-stack.md`](W0-US01-local-compose-stack.md) and [`W4-US06-grafana-provision.md`](W4-US06-grafana-provision.md).

### Logs

Console lines should be JSON (Logstash) including fields such as `@timestamp`, `level`, `logger_name`, `message`, and often `spring.application.name` / service fields — not free-form noise with credentials.

### Tests

```bash
./mvnw -pl dashpipe-api test -Dtest=PrometheusEndpointIT,StructuredLoggingSmokeTest
```

`PrometheusEndpointIT` needs Compose MySQL (same as other `local` profile ITs).

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 404 on `/actuator/prometheus` | Exposure list | `management.endpoints.web.exposure.include` must contain `prometheus` |
| Missing `jvm_memory_used_bytes` | Dependency | `micrometer-registry-prometheus` on classpath |
| Plain text logs only | Profile / yaml | Confirm `logging.structured.format.console=logstash` |

## Related

- Health: [`W0-US02-health-endpoint.md`](W0-US02-health-endpoint.md)
- Architecture §7 observability baseline
