# Dashflow

Cloud-assemblable, multi-tenant data pipeline platform (lifted from pipeline-platform).

## Documentation

| Document | Description |
|----------|-------------|
| [`docs/ASSEMBLIES.md`](docs/ASSEMBLIES.md) | How platform adapters are selected (local, Azure, …) |
| [`docs/AZURE_ASSEMBLY.md`](docs/AZURE_ASSEMBLY.md) | First cloud assembly: AKS, MySQL, Service Bus, Monitor |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | System architecture |
| [`docs/LOCALDEV_PIPELINE_GUIDE.md`](docs/LOCALDEV_PIPELINE_GUIDE.md) | Localdev + K8s pipelet runs |

## Assemblies

| Assembly | Broker | Database | Runtime |
|----------|--------|----------|---------|
| **local** (default) | RabbitMQ | MySQL (Compose) | local Kubernetes |
| **azure** | Azure Service Bus | Azure MySQL | AKS |

```yaml
dashflow:
  platform:
    broker: rabbitmq|servicebus
    runtime: kubernetes
    database: mysql            # postgresql reserved
    observability: prometheus|azure-monitor
```

Connectors/pipelets (S3, Blob, SQS, …) are **out of scope** for platform assembly — Phase 2.

## Modules

- `dashflow-spi` — `MessageBroker`, logical destinations
- `dashflow-broker-rabbitmq` — default adapter
- `dashflow-broker-servicebus` — Azure adapter
- `dashflow-api` — Spring Boot control plane
- `dashflow-ui` — React builder UI

## Getting started (local)

```bash
./scripts/localdev.sh start --with-metrics
./scripts/localdev.sh status
curl -s http://localhost:8080/actuator/health
```

Requires Docker, Java 21, Node 20+.

```bash
./mvnw -pl dashflow-api -am test
```
