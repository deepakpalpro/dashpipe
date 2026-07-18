---
layout: default
title: AI Agent and MCP
---

# AI Agent and MCP

Optional AI tooling for pipeline design and live control-plane operations.

## dashpipe-dev-ai-agent (Streamlit)

Standalone stack for:

| Feature | Description |
|---------|-------------|
| **Pipeline Guide** | Propose linear pipelet sequences from natural language; import bundles to the platform |
| **Pipelet Developer** | Generate Python, Dockerfile, and K8s Job YAML |
| **Platform Ops** | Full ops panel + LangGraph ops agent (pipelines, executions, connectors, catalog) |

The Streamlit app calls **shared `platform_ops` tools in-process** (same surface as MCP). It does **not** spawn the MCP server.

### Start

```bash
./dashpipe-dev-ai-agent/scripts/localdev-ai.sh start
```

| Service | URL |
|---------|-----|
| Dev agent (Streamlit) | http://localhost:8501 |
| AI API (optional REST) | http://localhost:8090 |
| Ollama | http://localhost:11434 |

Default models (16 GB Mac): `llama3.2:1b` (chat), `qwen2.5-coder:1.5b` (code).

Requires the **platform API** running separately for import/run/debug:

```bash
./dashpipe-ci_cd/scripts/localdev.sh start
```

See [dashpipe-dev-ai-agent/README.md](../../dashpipe-dev-ai-agent/README.md).

---

## dashpipe-mcp (MCP for Cursor, Claude, OpenClaw, GitHub Copilot)

Stdio MCP server exposing dashpipe-api as tools for AI clients. Same underlying `platform_ops` module as the Streamlit dev agent.

| Client | Config |
|--------|--------|
| **Cursor** | `.cursor/mcp.json` |
| **Claude Desktop** | `claude_desktop_config.json` |
| **OpenClaw** | `~/.openclaw/openclaw.json` or `openclaw mcp set` |
| **GitHub Copilot / VS Code** | `.vscode/mcp.json` (`servers` root key) |

Capabilities: list/run/debug pipelines, import/export bundles, manage connectors, browse pipelet catalog.

### Setup

1. Start platform API (`localdev.sh start`).
2. Install MCP server:

```bash
cd dashpipe-dev-ai-agent/dashpipe-mcp
python3 -m venv .venv && source .venv/bin/activate
pip install -e .
```

3. Copy an example config from [`dashpipe-mcp/examples/`](../../dashpipe-dev-ai-agent/dashpipe-mcp/examples/) and set the absolute Python path to your venv.

**Cursor** (`.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "dashpipe": {
      "command": "/absolute/path/to/dashpipe-dev-ai-agent/dashpipe-mcp/.venv/bin/python",
      "args": ["-m", "dashpipe_mcp"],
      "env": {
        "DASHPIPE_API_URL": "http://localhost:8080",
        "DASHPIPE_TENANT_ID": "T001"
      }
    }
  }
}
```

**VS Code / GitHub Copilot** (`.vscode/mcp.json` — note `servers`, not `mcpServers`):

```json
{
  "servers": {
    "dashpipe": {
      "type": "stdio",
      "command": "/absolute/path/to/dashpipe-dev-ai-agent/dashpipe-mcp/.venv/bin/python",
      "args": ["-m", "dashpipe_mcp"],
      "env": {
        "DASHPIPE_API_URL": "http://localhost:8080",
        "DASHPIPE_TENANT_ID": "T001"
      }
    }
  }
}
```

**Claude Desktop** — same `mcpServers` block as Cursor in `~/Library/Application Support/Claude/claude_desktop_config.json`.

**OpenClaw** — same block under `mcpServers` in `~/.openclaw/openclaw.json`, or:

```bash
openclaw mcp set dashpipe '{"command":"/path/to/.venv/bin/python","args":["-m","dashpipe_mcp"],"env":{"DASHPIPE_API_URL":"http://localhost:8080","DASHPIPE_TENANT_ID":"T001"}}'
```

Full reference + all example files: [dashpipe-mcp/README.md](../../dashpipe-dev-ai-agent/dashpipe-mcp/README.md).

### Typical workflow

1. Design pipeline in Streamlit Pipeline Guide → Import to platform
2. Activate, run, and debug in Platform Ops tab **or** via MCP from your IDE/agent host
3. Use `debug_execution` from Cursor, Claude, OpenClaw, or Copilot when iterating on failures
