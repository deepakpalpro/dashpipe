# Development Setup

Per-component development commands and layout.

## dashpipe-platform

| Module | Path | Stack |
|--------|------|-------|
| API | `dashpipe-platform/dashpipe-api` | Java 21, Spring Boot 3.4 |
| UI | `dashpipe-platform/dashpipe-ui` | React 19, Vite, TanStack Query |
| SPI | `dashpipe-platform/dashpipe-spi` | Java interfaces |
| Broker | `dashpipe-platform/dashpipe-broker` | RabbitMQ, Service Bus adapters |
| Pipelets | `dashpipe-platform/pipelets` | Python 3, Docker |

### Common commands

```bash
# Build and test API (+ dependencies)
./dashpipe-platform/mvnw -f dashpipe-platform -pl dashpipe-api -am test

# Run API only (after localdev started Compose deps)
cd dashpipe-platform
./mvnw -pl dashpipe-api spring-boot:run -Dspring-boot.run.profiles=local

# UI dev server
cd dashpipe-platform/dashpipe-ui
npm install && npx vite

# Lint UI
npm run lint
```

### Adding a pipelet

1. Create directory under `pipelets/{source|transformer|destination}/...`
2. Add `pipelet.json`, `main.py`, `Dockerfile`
3. Run `python3 dashpipe-ci_cd/scripts/generate_catalog_pipelets.py`
4. Build image: `./dashpipe-ci_cd/scripts/localdev.sh build-pipelets`

---

## dashpipe-ci_cd

| Area | Path |
|------|------|
| Localdev orchestrator | `scripts/localdev.sh` |
| Azure deploy | `scripts/azure/` |
| K8s manifests | `k8s/azure/`, `k8s/inventory/` |
| Bicep IaC | `azure/main.bicep` |

Test script changes by running localdev start/stop and relevant smoke scripts.

---

## dashpipe-dev-ai-agent

```bash
# AI stack (Ollama + API + UI)
./dashpipe-dev-ai-agent/scripts/localdev-ai.sh start

# API tests
cd dashpipe-dev-ai-agent/api
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt && pytest

# MCP tests
cd dashpipe-dev-ai-agent/dashpipe-mcp
pip install -e ".[dev]" && pytest
```

Knowledge files for prompts: `dashpipe-dev-ai-agent/api/app/knowledge/`

---

## dashpipe-tools

Edit Prometheus/Grafana/ELK configs under `dashpipe-tools/`. Verify with:

```bash
docker compose --profile metrics up -d
./dashpipe-tools/scripts/smoke-metrics.sh
```

---

## dashpipe-demo

Petstore and LocalStack mocks for E2E demos. Build via Compose profile `petstore` or localdev.

```bash
./dashpipe-demo/scripts/smoke-localstack.sh
```

---

## Docker Compose from repo root

All compose operations should run from the **repository root** so `include` paths resolve correctly.
