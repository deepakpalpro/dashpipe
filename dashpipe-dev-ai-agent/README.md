# AI Dataflow Developer (dashpipe-dev-ai-agent)

AI assistant for **Dataflow platform development** — design pipelines (pipelet sequences), generate **Python pipelets** with **Dockerfiles** and **Kubernetes** manifests, and operate the live platform (pipelines, executions, connectors, catalog).

Part of the **[dashpipe-suite](../README.md)** monorepo.

## Quick start

```bash
# Platform API (required for import/run/debug)
./dashpipe-ci_cd/scripts/localdev.sh start

# Dev agent stack
./dashpipe-dev-ai-agent/scripts/localdev-ai.sh start
# Streamlit: http://localhost:8501
# API:       http://localhost:8090
```

If you already run **Ollama on the host** (`:11434`), the start script detects it and skips the bundled Ollama container. To force host Ollama: `USE_HOST_OLLAMA=1 ./scripts/localdev-ai.sh start`. If port 11434 is taken by something else, bundled Ollama binds to `:11435` automatically.

## Architecture

```
Streamlit (:8501) ──in-process──► api/app agents
                                      │
                                      ▼
                              platform/tools.py ──► dashpipe_mcp/platform_ops.py ──► dashpipe-api (:8080)

Cursor MCP (stdio) ──► dashpipe_mcp/server.py ──► same platform_ops.py
Claude · OpenClaw · Copilot ──► same stdio server
```

The dev agent uses **shared in-process tools** (not an MCP subprocess). MCP is for external AI clients (Cursor, Claude Desktop, OpenClaw, GitHub Copilot / VS Code).

## Tabs

| Tab | Purpose |
|-----|---------|
| **Pipeline Guide** | Chat → proposed steps → import / dry-run / run |
| **Pipelet Developer** | Generate Python + Dockerfile + K8s YAML |
| **Platform Ops** | Status, pipelines, executions, connectors, catalog, ops chat agent |

## MCP (Cursor · Claude · OpenClaw · GitHub Copilot)

Live pipeline debug/import/run tools: [`dashpipe-mcp/README.md`](dashpipe-mcp/README.md) — example configs in [`dashpipe-mcp/examples/`](dashpipe-mcp/examples/).

## Stack

- **Streamlit UI** — Python (`8501`), imports `api/app` in-process
- **API** — Python 3.12 + FastAPI + LangChain + LangGraph (`8090`)
- **LLM** — Ollama (`11434`)

See [`api/requirements.txt`](api/requirements.txt) for dependencies.

## Manual E2E

1. `./dashpipe-ci_cd/scripts/localdev.sh start`
2. `./dashpipe-dev-ai-agent/scripts/localdev-ai.sh start`
3. Pipeline Guide → propose → Import → Activate → Run → Debug in Platform Ops tab
