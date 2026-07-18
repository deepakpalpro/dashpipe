# Vision

Dashpipe is an open-source, **multi-tenant data pipeline platform** for designing, running, and observing linear data flows without writing orchestration boilerplate.

## Problem

Teams building data integrations often stitch together scripts, cron jobs, and ad-hoc containers. That approach is hard to share across tenants, difficult to observe end-to-end, and expensive to operate at scale.

## Solution

Dashpipe provides:

| Capability | What it means |
|------------|---------------|
| **No-code pipeline builder** | Drag-and-drop linear pipelines in a React UI |
| **Pipelet execution model** | Each step runs as a containerized Python job on Kubernetes |
| **Connector SPI** | Reusable REST, storage, and message-bus connectors per tenant |
| **Pluggable assemblies** | Run locally (RabbitMQ + MySQL + K8s) or on Azure (AKS + Service Bus) |
| **Built-in observability** | Prometheus metrics, Grafana dashboards, execution logs |
| **Pay-as-you-go metering** | Usage events and billing hooks (Wave 5+) |

## Who is it for?

- **Data engineers** building repeatable Source → Transform → Destination flows
- **Platform teams** offering self-service pipelines to internal tenants
- **Integrators** connecting SaaS APIs, object storage, and message queues
- **Contributors** extending the pipelet catalog or connector SPI

## What Dashpipe is not

- A general-purpose workflow engine (DAGs, branching, and fan-out are limited)
- A managed cloud SaaS (this repo is self-hosted; you operate the control plane)
- A replacement for Spark/Flink batch processing at petabyte scale

## Monorepo components

Dashpipe ships as [dashpipe-suite](../README.md) with five top-level packages. See [Monorepo layout](../architecture/MONOREPO.md).

## Learn more

- [Use cases](USE_CASES.md) — concrete demo flows
- [Roadmap](ROADMAP.md) — delivery waves and status
- [Architecture overview](../architecture/OVERVIEW.md) — system layers
- [Full architecture](../../dashpipe-platform/docs/ARCHITECTURE.md) — API schemas, data model, SPI
