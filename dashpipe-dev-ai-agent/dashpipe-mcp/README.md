# Dashpipe MCP Server

Model Context Protocol (MCP) server for **dashpipe-api**. Exposes ~30 tools so AI clients can investigate pipelines, debug executions, import/export bundles, and manage connectors and pipelets.

Works with any **stdio MCP host**, including:

| Client | Config location |
|--------|-----------------|
| **Cursor** | `.cursor/mcp.json` (project) or user MCP settings |
| **Claude Desktop** | `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) |
| **OpenClaw** | `~/.openclaw/openclaw.json` → `mcpServers` (or `openclaw mcp set`) |
| **GitHub Copilot / VS Code** | `.vscode/mcp.json` (workspace) or user `mcp.json` |

Same server process everywhere — only the config file path and root key differ.

## Prerequisites

- **dashpipe-api** running (default `http://localhost:8080`)
- Python 3.12+
- Tenant header defaults to `T001` (same as localdev)

```bash
# From dashpipe repo root
./dashpipe-ci_cd/scripts/localdev.sh start
```

## Install

```bash
cd dashpipe-dev-ai-agent/dashpipe-mcp
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -e .
```

## Run (stdio)

```bash
python -m dashpipe_mcp
```

## Server block (copy into any client)

Replace `PYTHON` with the absolute path to this venv’s Python (e.g. `.../dashpipe-mcp/.venv/bin/python`):

```json
{
  "command": "PYTHON",
  "args": ["-m", "dashpipe_mcp"],
  "env": {
    "DASHPIPE_API_URL": "http://localhost:8080",
    "DASHPIPE_TENANT_ID": "T001"
  }
}
```

---

## Cursor

Project file `dashpipe-mcp/examples/cursor-mcp.json` or **Cursor Settings → MCP**:

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

---

## Claude Desktop

**Settings → Developer → Edit Config**, or edit directly:

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

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

Restart Claude Desktop after saving.

---

## OpenClaw

Add to `~/.openclaw/openclaw.json` under `mcpServers`, or use the CLI:

```bash
openclaw mcp set dashpipe '{
  "command": "/absolute/path/to/dashpipe-dev-ai-agent/dashpipe-mcp/.venv/bin/python",
  "args": ["-m", "dashpipe_mcp"],
  "env": {
    "DASHPIPE_API_URL": "http://localhost:8080",
    "DASHPIPE_TENANT_ID": "T001"
  }
}'
```

OpenClaw blocks some env vars (e.g. `PYTHONPATH`) in stdio configs for safety — set those on the host if needed, not in `env`.

Verify: `openclaw mcp list`

---

## GitHub Copilot / VS Code

Workspace config: `.vscode/mcp.json` (commit for your team). VS Code uses the root key **`servers`**, not `mcpServers`:

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

- Command Palette → **MCP: Open Workspace Folder MCP Configuration**
- Use **Agent** mode in Copilot Chat for tool calling
- Reload window after edits

Visual Studio 2026+ also reads `.mcp.json` at the repo root with the same stdio shape.

---

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `DASHPIPE_API_URL` | `http://localhost:8080` | dashpipe-api base URL |
| `DASHPIPE_TENANT_ID` | `T001` | `X-Tenant-Id` for tenant-scoped calls |
| `DASHPIPE_PIPELETS_CATALOG` | auto-resolved | Pipelet catalog JSON path |
| `DASHPIPE_REQUEST_TIMEOUT_S` | `60` | HTTP timeout |

## Tools

### Meta
- `dashpipe_health`, `dashpipe_config`

### Pipelines
- `list_pipelines`, `get_pipeline`, `create_pipeline`, `update_pipeline`
- `activate_pipeline`, `deactivate_pipeline`, `archive_pipeline`
- `replace_pipeline_steps`, `dry_run_pipeline`, `run_pipeline`
- `export_pipeline`, `import_pipeline`

### Executions & debug
- `list_executions`, `get_execution`, `cancel_execution`
- `get_execution_logs`, `debug_execution`

### Observability
- `get_pipeline_observability`, `get_observability_links`

### Connectors
- `list_connector_types`, `list_connectors`, `get_connector`
- `create_connector`, `update_connector`, `test_connector`
- `provision_connector_webhook`

### Pipelets (catalog)
- `list_pipelets`, `get_pipelet`

## Tests

```bash
pytest tests/
```

## Relation to AI Dataflow Developer

The **[dashpipe-dev-ai-agent](../)** Streamlit app designs pipelines and runs platform ops **in-process** (same `platform_ops` module). **dashpipe-mcp** is the stdio bridge for IDE and agent hosts — design in Streamlit, then import/run/debug from Cursor, Claude, OpenClaw, or Copilot.
