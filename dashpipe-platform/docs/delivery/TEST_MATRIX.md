# Test Coverage Matrix

Map each user story to required test types. Mark cells with `x` when Done, leave blank when pending, use `n/a` when not applicable for that story.

Parent plan: [`../DELIVERY_PLAN.md`](../DELIVERY_PLAN.md)  
Story template: [`STORY_TEMPLATE.md`](STORY_TEMPLATE.md)  
Wave TDD (stakeholders): [`tdd/README.md`](tdd/README.md)

| Columns | Meaning |
|---------|---------|
| **Unit** | Unit tests (TDD red/green) |
| **Integration** | Spring Boot IT / Testcontainers |
| **WireMock** | HTTP/API stubs |
| **LocalStack** | S3/SQS (or other LocalStack services) |
| **Manual** | Manual test steps executed |
| **KB** | Support KB article drafted/linked |

---

## Wave 0 — Foundation

| Story ID | Unit | Integration | WireMock | LocalStack | Manual | KB |
|----------|------|-------------|----------|------------|--------|-----|
| W0-US01 | n/a | n/a | n/a | x | x | x |
| W0-US02 | x | x | n/a | n/a | x | x |
| W0-US03 | x | x | n/a | n/a | x | x |
| W0-US04 | x | x | n/a | n/a | x | x |
| W0-US05 | x | n/a | x | n/a | x | x |

---

## Wave 1 — Tenancy, Services, Connectors

| Story ID | Unit | Integration | WireMock | LocalStack | Manual | KB |
|----------|------|-------------|----------|------------|--------|-----|
| W1-US01 | x | x | n/a | n/a | x | x |
| W1-US02 | x | x | n/a | n/a | x | x |
| W1-US03 | x | x | n/a | n/a | x | x |
| W1-US04 | x | x | n/a | n/a | x | x |
| W1-US05 | x | x | n/a | n/a | x | x |
| W1-US06 | x | x | x | n/a | x | x |
| W1-US07 | x | x | n/a | x | x | x |
| W1-US08 | x | x | n/a | x | x | x |

---

## Wave 2 — Pipelines & Ephemeral Execution

| Story ID | Unit | Integration | WireMock | LocalStack | Manual | KB |
|----------|------|-------------|----------|------------|--------|-----|
| W2-US01 | x | x | n/a | n/a | x | x |
| W2-US02 | x | x | n/a | n/a | x | x |
| W2-US03 | x | x | n/a | n/a | x | x |
| W2-US04 | x | x | n/a | n/a | x | x |
| W2-US05 | x | x | n/a | n/a | x | x |
| W2-US06 | x | x | n/a | n/a | x | x |
| W2-US07 | x | x | n/a | n/a | x | x |

---

## Wave 3 — Webhook Ingress + Queue

| Story ID | Unit | Integration | WireMock | LocalStack | Manual | KB |
|----------|------|-------------|----------|------------|--------|-----|
| W3-US01 | x | x | n/a | n/a | x | x |
| W3-US02 | x | x | n/a | n/a | x | x |
| W3-US03 | x | x | n/a | n/a | x | x |
| W3-US04 | x | x | n/a | n/a | x | x |
| W3-US05 | x | x | n/a | n/a | x | x |
| W3-US06 | x | x | n/a | n/a | x | x |
| W3-US07 | x | x | n/a | n/a | x | x |

---

## Wave 4 — Observability

| Story ID | Unit | Integration | WireMock | LocalStack | Manual | KB |
|----------|------|-------------|----------|------------|--------|-----|
| W4-US01 | x | x | n/a | n/a | x | x |
| W4-US02 | x | x | n/a | n/a | x | x |
| W4-US03 | x | | n/a | n/a | x | x |
| W4-US04 | x | | n/a | n/a | x | x |
| W4-US05 | x | x | n/a | n/a | x | x |
| W4-US06 | x | | n/a | n/a | x | x |

---

## Wave 5 — Metering & Pay-as-you-go

| Story ID | Unit | Integration | WireMock | LocalStack | Manual | KB |
|----------|------|-------------|----------|------------|--------|-----|
| W5-US01 | x | x | n/a | n/a | x | x |
| W5-US02 | x | x | n/a | n/a | x | x |
| W5-US03 | x | x | n/a | n/a | x | x |
| W5-US04 | x | x | n/a | n/a | x | x |
| W5-US05 | x | x | n/a | n/a | x | x |
| W5-US06 | x | x | n/a | n/a | x | x |

---

## Wave 6 — No-code UI

| Story ID | Unit | Integration | WireMock | LocalStack | Manual | KB |
|----------|------|-------------|----------|------------|--------|-----|
| W6-US01 | AuthContext.test, Shell.test | n/a | n/a | n/a | npm run dev shell smoke | [W6-US01-nav-shell](kb/W6-US01-nav-shell.md) |
| W6-US02 | ConnectorForm, ServiceForm, ConnectorsList, ServiceDetail, connectorListFilter, SearchableSelect | n/a | MSW handlers + seed fixtures | n/a | Create flows + secret mask + list filters | [W6-US02-connectors-services-ui](kb/W6-US02-connectors-services-ui.md) · [dual-config](kb/W6-dual-deployment-execution-config.md) |
| W6-US03 | catalogFilter, PipeletsCatalog, RegisterPipeletModal | n/a | MSW register stub | n/a | Filter + admin modal | [W6-US03-pipelets-catalog](kb/W6-US03-pipelets-catalog.md) |
| W6-US04 | pipelineGraphReducer, PipelineBuilder.save, StepPropertiesPanel, PipeletPalette, PipelinesListPage | n/a | MSW POST/PUT pipelines | n/a | 3-stage save smoke | [W6-US04-pipeline-builder](kb/W6-US04-pipeline-builder.md) |
| W6-US05 | executionOverlayReducer, ExecutionOverlay, RunControls.quota | PipelineDryRunIT | MSW run/poll/402/dry-run | n/a | Manual E2E in KB | [W6-US05-run-overlay](kb/W6-US05-run-overlay.md) |
| W6-US06 | CompletenessPanel, LatencyPanel, HeartbeatPanel | n/a | MSW obs series | n/a | Tab smoke | [W6-US06-observability-panels](kb/W6-US06-observability-panels.md) |

---

## Wave 7 — Hardening & Ops

| Story ID | Unit | Integration | WireMock | LocalStack | Manual | KB |
|----------|------|-------------|----------|------------|--------|-----|
| W7-US01 | | | n/a | n/a | | |
| W7-US02 | | | n/a | n/a | | |
| W7-US03 | | | n/a | n/a | | |
| W7-US04 | | | n/a | n/a | | |
| W7-US05 | n/a | n/a | n/a | n/a | | |
| W7-US06 | n/a | n/a | n/a | n/a | | |

---

## CI gate notes

- Prefer running Unit + Integration on every PR once Wave 0 harness exists.
- LocalStack and WireMock suites may run as a labeled job (`integration-external`) to keep PRs fast.
- Manual and KB remain human gates tracked here and in [`WAVE_TRACKER.md`](WAVE_TRACKER.md).
