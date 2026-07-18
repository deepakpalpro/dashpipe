# dashpipe-suite Documentation

Welcome to the Dashpipe documentation hub. Pick a path based on your role.

## By audience

| I am a… | Start here |
|---------|------------|
| **Executive / product stakeholder** | [Vision](overview/VISION.md) · [Roadmap](overview/ROADMAP.md) · [Use cases](overview/USE_CASES.md) |
| **Architect / tech lead** | [Architecture overview](architecture/OVERVIEW.md) · [Monorepo layout](architecture/MONOREPO.md) · [Full architecture](../dashpipe-platform/docs/ARCHITECTURE.md) |
| **Developer trying the product** | [Quickstart](getting-started/QUICKSTART.md) |
| **Platform engineer / SRE** | [Deployment](operations/DEPLOYMENT.md) · [Observability](operations/OBSERVABILITY.md) |
| **Open-source contributor** | [Contributing](../CONTRIBUTING.md) · [Development setup](contributing/DEVELOPMENT.md) |
| **AI / agent integrator** | [AI and MCP](getting-started/AI_AND_MCP.md) |

## Component docs

| Component | README |
|-----------|--------|
| Platform (API, UI, pipelets) | [dashpipe-platform/](../dashpipe-platform/) (see [docs/](../dashpipe-platform/docs/)) |
| CI/CD and deploy | [dashpipe-ci_cd/](../dashpipe-ci_cd/README.md) |
| AI agent + MCP | [dashpipe-dev-ai-agent/](../dashpipe-dev-ai-agent/README.md) |
| Observability stack | [dashpipe-tools/](../dashpipe-tools/README.md) |
| Demo mocks | [dashpipe-demo/](../dashpipe-demo/README.md) |

## Deep technical references

These live under `dashpipe-platform/docs/` and are the source of truth for implementation detail:

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](../dashpipe-platform/docs/ARCHITECTURE.md) | Full system design (API, data model, SPI, K8s) |
| [DELIVERY_PLAN.md](../dashpipe-platform/docs/DELIVERY_PLAN.md) | Incremental delivery waves W0–W7 |
| [LOCALDEV_PIPELINE_GUIDE.md](../dashpipe-platform/docs/LOCALDEV_PIPELINE_GUIDE.md) | Real K8s pipelet runs locally |
| [AZURE_ASSEMBLY.md](../dashpipe-platform/docs/AZURE_ASSEMBLY.md) | Azure AKS deployment guide |
| [TEST_MATRIX.md](../dashpipe-platform/docs/delivery/TEST_MATRIX.md) | Story × test-type coverage |

## Community

- [Contributing](../CONTRIBUTING.md)
- [Code of Conduct](../CODE_OF_CONDUCT.md)
- [Security policy](../SECURITY.md)
- [Changelog](../CHANGELOG.md)
