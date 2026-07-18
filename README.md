# dashpipe-suite

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![CI](https://github.com/your-org/dashpipe-suite/actions/workflows/platform.yml/badge.svg)](https://github.com/your-org/dashpipe-suite/actions)

**Dashpipe** is an open-source, multi-tenant data pipeline platform. Design linear Source → Transform → Destination flows in a no-code UI, execute them as Kubernetes pipelet jobs, and observe runs with Prometheus and Grafana.

Built for data engineers, platform teams, and integrators who need repeatable, tenant-scoped pipelines without orchestration boilerplate.

## Components

| Directory | Purpose |
|-----------|---------|
| [`dashpipe-platform/`](dashpipe-platform/) | Core product — API, UI, SPI, broker, pipelets, samples |
| [`dashpipe-ci_cd/`](dashpipe-ci_cd/) | Docker/K8s/Azure deploy, localdev orchestration, smoke/e2e scripts |
| [`dashpipe-dev-ai-agent/`](dashpipe-dev-ai-agent/) | AI pipeline/pipelet generation (LangGraph) + MCP control-plane tools |
| [`dashpipe-tools/`](dashpipe-tools/) | Observability — Prometheus, Grafana, ELK |
| [`dashpipe-demo/`](dashpipe-demo/) | Demo mocks — Petstore, LocalStack |

## Quick start

```bash
# Platform stack (Compose + API + UI)
./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics

# Optional: AI agent stack (Ollama + pipeline guide + pipelet developer)
./dashpipe-dev-ai-agent/scripts/localdev-ai.sh start

# Optional: real K8s pipelet runs
./dashpipe-ci_cd/scripts/localdev.sh start --k8s
```

| Service | URL |
|---------|-----|
| UI | http://127.0.0.1:5173 |
| API | http://localhost:8080 |
| Grafana | http://localhost:3000 (with `--with-metrics`) |
| AI agent UI | http://localhost:5174 |
| AI agent API | http://localhost:8090 |

Tenant header: `X-Tenant-Id: T001`

## Documentation

Full documentation hub: **[docs/README.md](docs/README.md)**

| Audience | Start here |
|----------|------------|
| Stakeholders | [Vision](docs/overview/VISION.md) · [Roadmap](docs/overview/ROADMAP.md) |
| New users | [Quickstart](docs/getting-started/QUICKSTART.md) |
| Contributors | [Contributing](CONTRIBUTING.md) |
| Operators | [Deployment](docs/operations/DEPLOYMENT.md) |

Deep technical references live under [`dashpipe-platform/docs/`](dashpipe-platform/docs/) (architecture, delivery plan, local K8s guide).

## Build and test

```bash
./dashpipe-platform/mvnw -f dashpipe-platform -pl dashpipe-api -am test
cd dashpipe-platform/dashpipe-ui && npm test
cd dashpipe-dev-ai-agent/dashpipe-mcp && pip install -e ".[dev]" && pytest
cd dashpipe-dev-ai-agent/api && pip install -r requirements.txt && pytest
```

## Deploy to Azure

See [docs/operations/DEPLOYMENT.md](docs/operations/DEPLOYMENT.md) and [dashpipe-platform/docs/AZURE_ASSEMBLY.md](dashpipe-platform/docs/AZURE_ASSEMBLY.md).

```bash
./dashpipe-ci_cd/scripts/azure/build-push-acr.sh <acrName> 0.1.0
./dashpipe-ci_cd/scripts/azure/apply-aks.sh <acrName> 0.1.0
```

## Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) and [docs/contributing/](docs/contributing/).

- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Security policy](SECURITY.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0 — see [LICENSE](LICENSE).
