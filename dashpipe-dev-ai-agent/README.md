# AI Dataflow Developer (dashpipe-dev-ai-agent)

AI assistant for **Dataflow platform development** — design pipelines (pipelet sequences) and generate **Python pipelets** with **Dockerfiles** and **Kubernetes** manifests.

Part of the **[dashpipe-suite](../README.md)** monorepo.

## Quick start

```bash
./dashpipe-dev-ai-agent/scripts/localdev-ai.sh start
# UI:  http://localhost:5174
# API: http://localhost:8090
```

Platform API (for MCP live ops): start with `./dashpipe-ci_cd/scripts/localdev.sh start`.

## MCP (Cursor)

Live pipeline debug/import/run tools: [`dashpipe-mcp/README.md`](dashpipe-mcp/README.md)

## Stack

- **API** — Python 3.12 + FastAPI + LangChain + LangGraph (`8090`)
- **UI** — React + Vite (`5174`)
- **LLM** — Ollama (`11434`)

See [`api/requirements.txt`](api/requirements.txt) for dependencies.
