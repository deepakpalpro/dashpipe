# Dashpipe MCP Server

Model Context Protocol (MCP) server for **dashpipe-api**. Exposes tools so Cursor (or any MCP client) can investigate pipelines, debug executions, import/export bundles, and manage connectors and pipelets.

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
cd dashpipe-mcp
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Run (stdio — for Cursor)

```bash
python -m dashpipe_mcp
```

## Cursor configuration

Add to your project or user MCP settings (`.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "dashpipe": {
      "command": "/Users/deepakpal/Projects/dashpipe/dashpipe-mcp/.venv/bin/python",
      "args": ["-m", "dashpipe_mcp"],
      "env": {
        "DASHPIPE_API_URL": "http://localhost:8080",
        "DASHPIPE_TENANT_ID": "T001"
      }
    }
  }
}
```

Adjust the Python path to your venv location.

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `DASHPIPE_API_URL` | `http://localhost:8080` | dashpipe-api base URL |
| `DASHPIPE_TENANT_ID` | `T001` | `X-Tenant-Id` for tenant-scoped calls |
| `DASHPIPE_PIPELETS_CATALOG` | `../dashpipe-ui/src/fixtures/pipelets.json` | Pipelet catalog JSON |
| `DASHPIPE_REQUEST_TIMEOUT_S` | `60` | HTTP timeout |

## Tools

### Meta
- `dashpipe_health` — API health check
- `dashpipe_config` — effective configuration

### Pipelines
- `list_pipelines`, `get_pipeline`, `create_pipeline`, `update_pipeline`
- `activate_pipeline`, `deactivate_pipeline`, `archive_pipeline`
- `replace_pipeline_steps`, `dry_run_pipeline`, `run_pipeline`
- `export_pipeline`, `import_pipeline`

### Executions & debug
- `list_executions`, `get_execution`, `cancel_execution`
- `get_execution_logs`, `debug_execution` (detail + logs + errors + links)

### Observability
- `get_pipeline_observability`, `get_observability_links`

### Connectors
- `list_connector_types`, `list_connectors`, `get_connector`
- `create_connector`, `update_connector`, `test_connector`
- `provision_connector_webhook`

### Pipelets (catalog)
- `list_pipelets`, `get_pipelet` — from generated `pipelets.json` fixture

## Tests

```bash
pytest tests/
```

## Relation to AI Dataflow Developer

The **[dashpipe-dev-ai-agent](../)** component generates pipeline designs and Python pipelets. This MCP server operates the **live Dashpipe control plane** — use both together: design in the AI UI, then create/import/run/debug via MCP tools in Cursor.
