# Sample pipeline bundles

Importable JSON for **Pipelines → Import** (or builder Export/Import).

| File | Pipeline |
|------|----------|
| [`inventory-s3-to-petstore.pipeline.json`](inventory-s3-to-petstore.pipeline.json) | LocalStack S3 CSV → filter/map → Petstore upload |

## Inventory S3 → Petstore

### Prerequisites

```bash
# From repo root
docker compose --profile petstore up -d mysql rabbitmq localstack petstore

# Seed the CSV object
./scripts/inventory-pipeline-e2e.sh --register-only   # or manually:
# awslocal s3 mb s3://demo-s3-source
# awslocal s3 cp mockservice/petstore/samples/inventory.csv s3://demo-s3-source/inventory/daily.csv

# Build pipelet images (for K8s / docker Jobs)
for p in plet-s3-source plet-csv-source plet-rest-source plet-csv-to-json plet-python-filter plet-field-mapper plet-webhook-destination; do
  docker build -f pipelets/$p/Dockerfile -t "dashflow/${p}:local" pipelets
done

# API (+ optional k8s profile for real Jobs)
./mvnw -pl dashflow-api -am install -DskipTests
./mvnw -pl dashflow-api spring-boot:run -Dspring-boot.run.profiles=local
# or: ... -Dspring-boot.run.profiles=local,k8s
```

Prefer the full walkthrough: [`docs/LOCALDEV_PIPELINE_GUIDE.md`](../../docs/LOCALDEV_PIPELINE_GUIDE.md) (`./scripts/localdev.sh start --k8s --with-metrics`).

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
