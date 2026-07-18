# Roadmap

Public summary of delivery waves. Internal story tracking lives in [DELIVERY_PLAN.md](../../dashpipe-platform/docs/DELIVERY_PLAN.md) and [WAVE_TRACKER.md](../../dashpipe-platform/docs/delivery/WAVE_TRACKER.md).

## Wave status

| Wave | Theme | Status | Highlights |
|------|-------|--------|------------|
| **W0** | Foundation | Done | Local Compose (MySQL, RabbitMQ, LocalStack), structured logging, Prometheus scrape |
| **W1** | Tenancy, services, connectors | Done | Tenant isolation, connector SPI, REST/storage/message-bus connectors |
| **W2** | Pipelines and execution | Done | Pipeline CRUD, K8s pipelet jobs, run/cancel, import/export bundles |
| **W3** | Webhook ingress | Done | Public webhook URLs, signature verification, idempotency |
| **W4** | Observability | Done | Execution logs, Grafana dashboards, ELK profile, completeness metrics |
| **W5** | Metering and PAYG | Done | Usage events, credit balance, quota enforcement |
| **W6** | No-code UI polish | In progress | Builder UX, execution debug panel, observability panels |
| **W7** | Hardening and ops | Planned | Production runbooks, HA patterns, extended cloud assemblies |

## Near-term focus

1. **Wave 6 completion** — builder and observability UX
2. **Pipelet catalog depth** — many catalog entries are scaffolds; priority pipelets get full runtime implementations (see [PIPELET_IMPLEMENTATION_ROADMAP.md](../../dashpipe-platform/docs/PIPELET_IMPLEMENTATION_ROADMAP.md))
3. **Azure Wave B** — managed MySQL + Service Bus instead of in-cluster deps
4. **Open-source community** — contributor docs, issue templates, first tagged release

## Out of scope (current phase)

- Multi-region active-active control plane
- Apache Kafka as default platform broker (RabbitMQ / Service Bus today)
- Embedded AI in the main Dashpipe UI (AI agent remains a separate component)

## How to influence the roadmap

- Open a [feature request](https://github.com/your-org/dashpipe-suite/issues/new?template=feature_request.yml)
- Comment on an existing issue with your use case and environment
- Contribute a pipelet or connector — see [Contributing](../../CONTRIBUTING.md)
