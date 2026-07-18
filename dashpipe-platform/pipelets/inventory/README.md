# Inventory pipelets — S3 CSV → Petstore

Dockerized pipelets that implement the daily inventory load:

1. **S3 Source** (`plet-s3-source`) — pull `inventory/daily.csv` from LocalStack/S3  
2. **CSV → JSON** (`plet-csv-to-json`) — parse rows  
3. **Filter** (`plet-python-filter`) — category ∈ {food, accessories, toys}, quantity > 0, valid SKU  
4. **Map** (`plet-field-mapper`) — `{ "mode": "upsert", "items": [...] }`  
5. **Load** (`plet-webhook-destination`) — `POST /api/v3/inventory/upload`

## Quick e2e (Docker)

```bash
# From repo root — Compose deps + Petstore + S3 seed + batch pipelet
./dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh
```

Chained stages (one container per pipelet):

```bash
./dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh --stages
```

Kubernetes (Kind if available):

```bash
./dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh --k8s
```

## Image

```bash
docker build -t dashpipe/inventory-pipelet:local pipelets/inventory
```

| Env | Purpose |
|-----|---------|
| `RUN_MODE` | `batch` (default) or `stage` |
| `PIPELET_ID` | Catalog id when `RUN_MODE=stage` |
| `S3_CONNECTOR_CONFIG` | JSON: bucket, endpoint, region, keys |
| `REST_CONNECTOR_CONFIG` | JSON: baseUrl for Petstore |
| `EXECUTION_CONFIG` | JSON: objectKey, mode, path |

## K8s

Manifest: [`dashpipe-ci_cd/k8s/inventory/inventory-pipeline.yaml`](../../../dashpipe-ci_cd/k8s/inventory/inventory-pipeline.yaml)

Namespace `tenant-t001`, Job `inventory-s3-to-petstore`.
