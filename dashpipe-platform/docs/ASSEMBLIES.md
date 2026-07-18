# Dashpipe Assemblies

Dashpipe is a **cloud-assemblable** pipeline platform: a stable kernel plus swappable adapters.

## Kernel vs assembly

| Layer | Responsibility | Examples |
|-------|----------------|----------|
| **Kernel** | API, orchestration, logical destination names, tenants, billing | `dashpipe-api`, `dashpipe-spi` |
| **Assembly** | Concrete cloud/tech choices for broker, DB host, runtime, observability | local, azure (aws/gcp later) |

Tenant **connectors** and **pipelets** (S3, Blob, SQS, …) are **not** part of the platform assembly. They stay pluggable per pipeline (Phase 2).

## Configuration

```yaml
dashpipe:
  platform:
    broker: rabbitmq          # or servicebus
    runtime: kubernetes            # Jobs via Fabric8 (AKS/EKS/GKE/local)
    database: mysql           # postgresql reserved for a later adapter
    observability: prometheus # or azure-monitor
```

Select the Spring profile that matches the assembly (`local`, `azure`, …).

## Modules

| Module | Role |
|--------|------|
| `dashpipe-spi` | `MessageBroker`, `LogicalDestinations`, `PlatformAssembly` |
| `dashpipe-broker` | MessageBroker adapters (RabbitMQ default / local; Service Bus for Azure) |
| `dashpipe-api` | Control plane |

Only one `MessageBroker` bean is active (`@ConditionalOnProperty dashpipe.platform.broker`).

## Shipped assemblies

| Assembly | Broker | Database | Runtime | Observability |
|----------|--------|----------|---------|---------------|
| **local** (default) | RabbitMQ | MySQL (Compose) | local K8s / Rancher | Prometheus |
| **azure** | Service Bus | Azure MySQL Flexible Server | AKS | Azure Monitor / Log Analytics |

See [AZURE_ASSEMBLY.md](AZURE_ASSEMBLY.md) and [`deploy/assemblies/`](../deploy/assemblies/).

## Adding AWS / GCP later

1. Implement `MessageBroker` (e.g. SQS or Pub/Sub) in `dashpipe-broker-*`.
2. Add `application-aws.yml` / `application-gcp.yml` + IaC under `deploy/aws` / `deploy/gcp`.
3. Pin the stack in `deploy/assemblies/<name>.yaml`.
4. Keep `LogicalDestinations` names unchanged.
