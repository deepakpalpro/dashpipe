# Sample pipeline bundles

Importable JSON for **Pipelines → Import** (or builder Export/Import).

| File | Pipeline |
|------|----------|
| [`manual-trigger-s3-out.pipeline.json`](manual-trigger-s3-out.pipeline.json) | Click **Run**: manual → filter → LocalStack S3 out |
| [`inventory-schedule-sync.pipeline.json`](inventory-schedule-sync.pipeline.json) | Every 15m: schedule → petstore-inventory (:4011) → petstore (:4010) |
| [`inventory-s3-to-petstore.pipeline.json`](inventory-s3-to-petstore.pipeline.json) | LocalStack S3 CSV → filter/map → Petstore upload |
| [`rest-source-demo.pipeline.json`](rest-source-demo.pipeline.json) | REST pull (:4011) → filter → Petstore upload (:4010) |

## Manual Trigger → S3 Out

On-demand demo: **Activate** → optionally paste JSON in **Trigger payload** → **Run** (trigger=`manual`). No cron.

### Pipelets (active in catalog)

- `plet-manual-source` — emits your Run JSON body (or a default kickoff if empty)
- `plet-python-filter` — pass-through
- `plet-s3-destination` — writes JSON to LocalStack S3

### Trigger payload

```bash
# UI: paste into "Trigger payload (JSON)" then Run
# or API:
curl -sS -X POST "http://localhost:8080/api/v1/pipelines/<id>/run" \
  -H "X-Tenant-Id: T001" -H "Content-Type: application/json" \
  -d '{"payload":{"sku":"food-01","qty":3}}'
```

The API injects that JSON as `TRIGGER_PAYLOAD` on the Manual Source Job.

### Prerequisites

```bash
./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics --k8s --with-elk --no-build-pipelets

# Ensure LocalStack bucket exists
awslocal s3 mb s3://demo-file-destination 2>/dev/null || true

# Build pipelets if images are missing
for p in plet-manual-source plet-python-filter plet-s3-destination; do
  docker build -t "dashpipe/${p}:local" -f "dashpipe-platform/pipelets/$(find dashpipe-platform/pipelets -type d -name "$p" | head -1 | sed "s|.*/pipelets/||")/Dockerfile" dashpipe-platform/pipelets
done
```

### Import in UI

1. **Pipelines** → **Import** → `samples/pipelines/manual-trigger-s3-out.pipeline.json`
2. Open the pipeline → **Activate** → **Run**
3. Verify the object:
   ```bash
   awslocal s3 ls s3://demo-file-destination/manual/
   awslocal s3 cp s3://demo-file-destination/manual/trigger-out.json -
   ```

### Notes

- Connector uses LocalStack (`host.docker.internal:4567`) so K8s Jobs can reach Compose on the host.
- `plet-file-destination` writes to a **mounted/local path** (`basePath` + `objectKey`); use `plet-s3-destination` for S3-compatible buckets.
- Unlike the schedule sample, this never fires from `PipelineSchedulePoller` — only **Run**.

## Inventory Sync every 15 minutes

Schedule-driven sync from **petstore-inventory** (`:4011`) into **petstore** (`:4010`).

### Pipelets (active in catalog)

- `plet-schedule-source` — cron tick (`*/15 * * * *`)
- `plet-rest-source` — `GET /inventory/items` from `:4011`
- `plet-python-filter` — pass-through
- `plet-webhook-destination` — `POST /inventory/upload` to `:4010`

Pipeline `execution_config.scheduleCron` is synced to `pipelines.schedule_cron`; the API `PipelineSchedulePoller` starts an ACTIVE pipeline on matching minutes (trigger=`schedule`).

### Prerequisites

```bash
# From repo root — petstore + catalog mock
./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics --k8s --with-elk --no-build-pipelets
# Ensure Compose petstore profile is up (ports 4010 + 4011)

# Build pipelets used by this sample (if images missing)
for p in plet-schedule-source plet-rest-source plet-python-filter plet-webhook-destination; do
  docker build -f "dashpipe-platform/pipelets/$(find dashpipe-platform/pipelets -type d -name "$p" | head -1 | sed "s|.*/pipelets/||")/Dockerfile" \
    -t "dashpipe/${p}:local" dashpipe-platform/pipelets 2>/dev/null \
    || ./dashpipe-ci_cd/scripts/localdev.sh build-pipelets
done
```

Prefer: `./dashpipe-ci_cd/scripts/localdev.sh build-pipelets` once.

### Import in UI

1. Open **Pipelines** → **Import** → choose `samples/pipelines/inventory-schedule-sync.pipeline.json`
2. Open the pipeline → **Activate** (status ACTIVE) so the schedule poller can fire it
3. Optional: **Run** once immediately to verify pull → upload
4. Watch **Observability** / builder Debug for runs every 15 minutes

### Notes

- Cron: `*/15 * * * *` (classic 5-field; platform adds seconds for Spring).
- Connector endpoints use `host.docker.internal` so K8s Jobs reach Compose on the host.
- Disable the poller with `pipeline.schedule.enabled=false` if needed.

## Inventory S3 → Petstore

### Prerequisites

```bash
# From repo root
docker compose --profile petstore up -d mysql rabbitmq localstack petstore

# Seed the CSV object
./dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh --register-only   # or manually:
# awslocal s3 mb s3://demo-s3-source
# awslocal s3 cp dashpipe-demo/petstore/samples/inventory.csv s3://demo-s3-source/inventory/daily.csv

# Build pipelet images (for K8s / docker Jobs)
for p in plet-s3-source plet-csv-source plet-rest-source plet-csv-to-json plet-python-filter plet-field-mapper plet-webhook-destination; do
  rel=$(find dashpipe-platform/pipelets -type d -name "$p" | head -1)
  docker build -f "$rel/Dockerfile" -t "dashpipe/${p}:local" dashpipe-platform/pipelets
done

# API (+ optional k8s profile for real Jobs)
cd dashpipe-platform
./mvnw -pl dashpipe-api -am install -DskipTests
./mvnw -pl dashpipe-api spring-boot:run -Dspring-boot.run.profiles=local
# or: ... -Dspring-boot.run.profiles=local,k8s
```

Prefer the full walkthrough: [`docs/LOCALDEV_PIPELINE_GUIDE.md`](../../docs/LOCALDEV_PIPELINE_GUIDE.md) (`./dashpipe-ci_cd/scripts/localdev.sh start --k8s --with-metrics`).

### Import in UI

1. Open **Pipelines**
2. Click **Import** and choose `samples/pipelines/inventory-s3-to-petstore.pipeline.json`
3. Leave **Reuse existing connectors/services** checked if you already have matching names; otherwise uncheck to create new ones
4. Open the imported pipeline → **Deploy** → **Run**
5. Use the builder **Debug / logs** panel for status, indexed logs, and kubectl hints

### Notes

- Connector endpoints use `host.docker.internal` so Rancher Desktop Jobs can reach Compose LocalStack (:4567) and Petstore (:4010) on the host. For API-only / stub runs on the host, `localhost` also works.
- Pipeline `execution_config.ioMode` is `queue` (platform default). Use `stdio` for local pipe chaining only.
- With `local,k8s`, Jobs receive `CONNECTOR_CONFIG` / `EXECUTION_CONFIG` / etc., and a status poller marks runs **failed** on Job backoff or **completed** after the last stage succeeds.
