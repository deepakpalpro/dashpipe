# dashflow-suite

Monorepo for the Dashflow data pipeline platform and supporting tooling.

## Components

| Directory | Purpose |
|-----------|---------|
| [`dashflow-platform/`](dashflow-platform/) | Core product — API, UI, SPI, broker, pipelets, samples |
| [`dashflow-ci_cd/`](dashflow-ci_cd/) | Docker/K8s/Azure deploy, localdev orchestration, smoke/e2e scripts |
| [`dashflow-dev-ai-agent/`](dashflow-dev-ai-agent/) | AI pipeline/pipelet generation (LangGraph) + MCP control-plane tools |
| [`dashflow-tools/`](dashflow-tools/) | Observability — Prometheus, Grafana, ELK |
| [`dashflow-demo/`](dashflow-demo/) | Demo mocks — Petstore, LocalStack |

## Quick start

```bash
# Platform stack (Compose + API + UI)
./dashflow-ci_cd/scripts/localdev.sh start --with-metrics

# Optional: AI agent stack (Ollama + pipeline guide + pipelet developer)
./dashflow-dev-ai-agent/scripts/localdev-ai.sh start

# Optional: real K8s pipelet runs
./dashflow-ci_cd/scripts/localdev.sh start --k8s
```

| Service | URL |
|---------|-----|
| UI | http://127.0.0.1:5173 |
| API | http://localhost:8080 |
| Grafana | http://localhost:3000 (with `--with-metrics`) |
| AI agent UI | http://localhost:5174 |
| AI agent API | http://localhost:8090 |

Tenant header: `X-Tenant-Id: T001`

## Build & test

```bash
./dashflow-platform/mvnw -f dashflow-platform -pl dashflow-api -am test
cd dashflow-platform/dashflow-ui && npm test
cd dashflow-dev-ai-agent/dashflow-mcp && pip install -e ".[dev]" && pytest
cd dashflow-dev-ai-agent/api && pip install -r requirements.txt && pytest
```

## Deploy to Azure

See [`dashflow-platform/docs/AZURE_ASSEMBLY.md`](dashflow-platform/docs/AZURE_ASSEMBLY.md) and [`dashflow-ci_cd/k8s/azure/README.md`](dashflow-ci_cd/k8s/azure/README.md).

```bash
./dashflow-ci_cd/scripts/azure/build-push-acr.sh <acrName> 0.1.0
./dashflow-ci_cd/scripts/azure/apply-aks.sh <acrName> 0.1.0
```

## Docker Compose

Root [`docker-compose.yml`](docker-compose.yml) includes platform, demo, and tools stacks via Compose `include`.

```bash
docker compose --profile petstore --profile metrics up -d
```

## Documentation

Platform architecture and guides live under [`dashflow-platform/docs/`](dashflow-platform/docs/).
