# Testing

How to run tests and what CI expects.

## CI workflows

| Workflow | Trigger | What it runs |
|----------|---------|--------------|
| [platform.yml](../../.github/workflows/platform.yml) | Push/PR | Maven test, UI test + lint |
| [ai-agent.yml](../../.github/workflows/ai-agent.yml) | Push/PR | AI API pytest, UI build |
| [mcp.yml](../../.github/workflows/mcp.yml) | Push/PR | MCP pytest |
| [smoke-compose.yml](../../.github/workflows/smoke-compose.yml) | Nightly / manual | Compose deps smoke |

## Platform (Java)

```bash
./dashpipe-platform/mvnw -f dashpipe-platform -pl dashpipe-api -am test
```

Integration tests (`*IT.java`) require Docker for Testcontainers / Compose MySQL on port 3306. Tests skip gracefully when MySQL is unavailable.

## Platform (UI)

```bash
cd dashpipe-platform/dashpipe-ui
npm install
npm test -- --run
npm run lint
```

**Note:** 7 UI tests currently fail (quota-alert panel) — pre-existing, unrelated to most changes. Fix if your PR touches billing/quota UI.

## AI agent

```bash
cd dashpipe-dev-ai-agent/api && pip install -r requirements.txt && pytest
```

## MCP

```bash
cd dashpipe-dev-ai-agent/dashpipe-mcp && pip install -e ".[dev]" && pytest
```

## Smoke and E2E scripts

Run manually after starting the stack:

```bash
./dashpipe-ci_cd/scripts/smoke-compose-deps.sh
./dashpipe-ci_cd/scripts/smoke-localstack.sh
./dashpipe-ci_cd/scripts/smoke-metrics.sh
./dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh --register-only
```

## Test matrix

Story-level coverage expectations: [TEST_MATRIX.md](../../dashpipe-platform/docs/delivery/TEST_MATRIX.md)

When completing a user story, update the matrix and add/update KB articles per [STORY_TEMPLATE.md](../../dashpipe-platform/docs/delivery/STORY_TEMPLATE.md).

## Before opening a PR

- [ ] Tests pass for components you changed
- [ ] No new linter errors
- [ ] Docs updated if behavior or paths changed
