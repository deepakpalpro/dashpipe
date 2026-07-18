# Dashflow MCP Server

Model Context Protocol (MCP) server for **dashflow-api**. Exposes tools so Cursor (or any MCP client) can investigate pipelines, debug executions, import/export bundles, and manage connectors and pipelets.

## Prerequisites

- **dashflow-api** running (default `http://localhost:8080`)
- Python 3.12+
- Tenant header defaults to `T001` (same as localdev)

```bash
# From dashflow repo root
./dashflow-ci_cd/scripts/localdev.sh start
```

## Install

```bash
cd dashflow-mcp
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Run (stdio — for Cursor)

```bash
python -m dashflow_mcp
```

## Cursor configuration

Add to your project or user MCP settings (`.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "dashflow": {
      "command": "/Users/deepakpal/Projects/dashflow/dashflow-mcp/.venv/bin/python",
      "args": ["-m", "dashflow_mcp"],
      "env": {
        "DASHFLOW_API_URL": "http://localhost:8080",
        "DASHFLOW_TENANT_ID": "T001"
      }
    }
  }
}
```

Adjust the Python path to your venv location.

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `DASHFLOW_API_URL` | `http://localhost:8080` | dashflow-api base URL |
| `DASHFLOW_TENANT_ID` | `T001` | `X-Tenant-Id` for tenant-scoped calls |
| `DASHFLOW_PIPELETS_CATALOG` | `../dashflow-ui/src/fixtures/pipelets.json` | Pipelet catalog JSON |
| `DASHFLOW_REQUEST_TIMEOUT_S` | `60` | HTTP timeout |

## Tools

### Meta
- `dashflow_health` — API health check
- `dashflow_config` — effective configuration

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

The **[dashflow-dev-ai-agent](../)** component generates pipeline designs and Python pipelets. This MCP server operates the **live Dashflow control plane** — use both together: design in the AI UI, then create/import/run/debug via MCP tools in Cursor.
