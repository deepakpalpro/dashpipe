# dashpipe-demo

Mock data-plane services for end-to-end pipeline demos.

## Contents

| Path | Purpose |
|------|---------|
| [petstore/](petstore/) | Petstore inventory upload sink (port 4010) |
| [petstore-inventory/](petstore-inventory/) | GET-only inventory catalog for REST source demos (port 4011) |
| [localstack/](localstack/) | LocalStack notes (S3/SQS via upstream image) |
| [k8s/](k8s/) | Petstore manifests for AKS overlay |
| [docker-compose.demo.yml](docker-compose.demo.yml) | LocalStack + Petstore services |
| [scripts/smoke-localstack.sh](scripts/smoke-localstack.sh) | S3/SQS smoke test |

## Enable locally

From repo root:

```bash
docker compose --profile petstore up -d
```

Or use localdev (starts petstore profile by default):

```bash
./dashpipe-ci_cd/scripts/localdev.sh start
```

| Service | URL |
|---------|-----|
| Petstore | http://localhost:4010 |
| Petstore Inventory | http://localhost:4011 |
| LocalStack | http://localhost:4567 |

## Flagship demo

Inventory CSV from S3 to Petstore — see [docs/overview/USE_CASES.md](../docs/overview/USE_CASES.md).

```bash
./dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh
```

## Docs

- [Sample pipelines](../dashpipe-platform/samples/pipelines/README.md)
- [Local development](../docs/getting-started/LOCAL_DEVELOPMENT.md)

**Note:** Demo services are for development and testing, not production deployment.
