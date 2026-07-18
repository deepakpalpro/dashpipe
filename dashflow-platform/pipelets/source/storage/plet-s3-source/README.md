# plet-s3-source — generic S3 Source pipelet

Fetches one object from an S3-compatible store (AWS or LocalStack). Values are
resolved from **connector + service + pipeline step** KeyValue maps.

## Required Keys (after merge)

| Layer | Key | Source |
|-------|-----|--------|
| **Deployment** | `region` | Step / pipeline deployment KeyValue (or connector) |
| **Execution** | `objectKey` | Step / pipeline execution KeyValue (`key` alias accepted) |
| **Connector** | `bucket` | Bound storage connector config |

Optional: `endpoint` (LocalStack or Azure Blob S3 API), credentials from **service** or connector
(`accessKeyId` / `secretAccessKey`). For Azure, set `account` / `container` on the storage
connector (see Flyway `ct-storage` schema) and an HTTPS blob endpoint when not using S3-compat.

## Merge order

```text
defaults (pipelet.json)
  → CONNECTOR_CONFIG (bucket, endpoint, …)
  → SERVICE_CONFIG (credentials)
  → DEPLOYMENT_CONFIG / EXECUTION_CONFIG (pipeline step overrides)
```

## Configure in the UI

1. Create a **storage** connector with `bucket` (+ LocalStack `endpoint` if needed).
2. Optionally create an **Auth** service with access/secret keys.
3. Add **S3 Source** from the palette (active pipelet).
4. Bind connector (+ service) on the step.
5. Set step **Deployment** `region` and **Execution** `objectKey`
   (e.g. `inventory/daily.csv`).

## Run locally

```bash
docker build -f pipelets/plet-s3-source/Dockerfile -t dashflow/plet-s3-source:local pipelets

docker run --rm --network dashflow_default \
  -e IO_MODE=stdio \
  -e CONNECTOR_CONFIG='{"bucket":"demo-s3-source","endpoint":"http://localstack:4566","region":"us-east-1","accessKeyId":"test","secretAccessKey":"test"}' \
  -e SERVICE_CONFIG='{}' \
  -e DEPLOYMENT_CONFIG='{"region":"us-east-1"}' \
  -e EXECUTION_CONFIG='{"objectKey":"inventory/daily.csv"}' \
  dashflow/plet-s3-source:local
```

Emits a single JSON record via stdout (`IO_MODE=stdio`) or `OUTPUT_QUEUE` (`IO_MODE=queue`).
See [`../REGISTRY.md`](../REGISTRY.md) for the env contract.

## Tests

```bash
cd pipelets/plet-s3-source && python -m unittest test_config.py -v
```
