# AI Agent and MCP

Optional AI tooling for pipeline design and live control-plane operations.

## dashpipe-dev-ai-agent

Standalone stack for:

| Feature | Description |
|---------|-------------|
| **Pipeline Guide** | Propose linear pipelet sequences from natural language |
| **Pipelet Developer** | Generate Python, Dockerfile, and K8s Job YAML |

### Start

```bash
./dashpipe-dev-ai-agent/scripts/localdev-ai.sh start
```

| Service | URL |
|---------|-----|
| AI UI | http://localhost:5174 |
| AI API | http://localhost:8090 |
| Ollama | http://localhost:11434 |

Default models (16 GB Mac): `llama3.2:1b` (chat), `qwen2.5-coder:1.5b` (code).

Requires the **platform API** running separately for importing pipelines into the builder:

```bash
./dashpipe-ci_cd/scripts/localdev.sh start
```

See [dashpipe-dev-ai-agent/README.md](../../dashpipe-dev-ai-agent/README.md).

---

## dashpipe-mcp (Cursor integration)

MCP server exposing dashpipe-api as tools for AI agents in Cursor:

- List/run/debug pipelines
- Import/export bundles
- Manage connectors
- Browse pipelet catalog

### Setup

1. Start platform API (`localdev.sh start`).
2. Install MCP server:

```bash
cd dashpipe-dev-ai-agent/dashpipe-mcp
python3 -m venv .venv && source .venv/bin/activate
pip install -e .
```

3. Add to Cursor MCP settings (`.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "dashpipe": {
      "command": "/absolute/path/to/dashpipe-suite/dashpipe-dev-ai-agent/dashpipe-mcp/.venv/bin/python",
      "args": ["-m", "dashpipe_mcp"],
      "env": {
        "DASHPIPE_API_URL": "http://localhost:8080",
        "DASHPIPE_TENANT_ID": "T001"
      }
    }
  }
}
```

Full tool reference: [dashpipe-mcp/README.md](../../dashpipe-dev-ai-agent/dashpipe-mcp/README.md).

### Typical workflow

1. Design pipeline in AI UI → export JSON
2. Import via MCP `import_pipeline` or manually in Dashpipe builder
3. Activate, run, and debug with MCP `debug_execution`
