# Use Cases

Concrete flows you can run today with the local dev stack.

## 1. Inventory CSV from S3 to Petstore (flagship demo)

**Flow:** LocalStack S3 → S3 source pipelet → CSV/JSON transforms → webhook to Petstore inventory API.

| Asset | Location |
|-------|----------|
| Sample pipeline JSON | [inventory-s3-to-petstore.pipeline.json](../../dashpipe-platform/samples/pipelines/inventory-s3-to-petstore.pipeline.json) |
| E2E script | [inventory-pipeline-e2e.sh](../../dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh) |
| Demo services | [dashpipe-demo/petstore](../../dashpipe-demo/petstore/), LocalStack in Compose |

**Try it:**

```bash
./dashpipe-ci_cd/scripts/localdev.sh start --k8s --with-metrics
./dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh --register-only
# Import sample in UI → Deploy → Run
```

See [LOCALDEV_PIPELINE_GUIDE.md](../../dashpipe-platform/docs/LOCALDEV_PIPELINE_GUIDE.md) for verification steps (K8s Jobs, Petstore inventory counts).

---

## 2. Manual trigger to object storage

**Flow:** Manual source pipelet → field mapping / filter → S3 or file destination.

| Asset | Location |
|-------|----------|
| Sample pipeline | [manual-trigger-s3-out.pipeline.json](../../dashpipe-platform/samples/pipelines/manual-trigger-s3-out.pipeline.json) |

Good for testing a minimal two- or three-step pipeline without external APIs.

---

## 3. REST source ingestion

**Flow:** REST connector + REST source pipelet → JSON transform → destination (webhook, S3, or database pipelet).

| Asset | Location |
|-------|----------|
| Sample pipeline | [rest-source-demo.pipeline.json](../../dashpipe-platform/samples/pipelines/rest-source-demo.pipeline.json) |
| Inventory catalog mock | [petstore-inventory](../../dashpipe-demo/petstore-inventory/) on port 4011 |

Requires a REST connector configured in the builder with `baseUrl` pointing at the mock service.

---

## 4. Webhook-driven ingress (Wave 3)

**Flow:** External system POSTs to a provisioned webhook URL → event stored → pipeline triggered.

Documented in [ARCHITECTURE.md §11](../../dashpipe-platform/docs/ARCHITECTURE.md) and Wave 3 TDD docs. Requires an event-listener connector and active pipeline.

---

## 5. AI-assisted pipeline design (optional)

**Flow:** Describe intent in natural language → AI proposes pipelet sequence → export JSON → import into Dashpipe builder.

| Component | Location |
|-----------|----------|
| AI agent UI | [dashpipe-dev-ai-agent](../../dashpipe-dev-ai-agent/) |
| MCP live operations | [dashpipe-mcp](../../dashpipe-dev-ai-agent/dashpipe-mcp/) |

See [AI and MCP](../getting-started/AI_AND_MCP.md).

---

## Sample pipeline index

All bundled samples: [dashpipe-platform/samples/pipelines/README.md](../../dashpipe-platform/samples/pipelines/README.md)
