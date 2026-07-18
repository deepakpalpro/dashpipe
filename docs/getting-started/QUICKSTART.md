# Quickstart

Get Dashpipe running locally in about 10 minutes.

## Prerequisites

| Tool | Version |
|------|---------|
| Docker | 24+ with Compose v2.20+ (for `include` support) |
| Java | 21 |
| Node.js | 20+ |
| Git | any recent |

Optional for real pipelet runs: **Rancher Desktop** or another local Kubernetes cluster.

## 1. Clone and start

```bash
git clone https://github.com/your-org/dashpipe-suite.git
cd dashpipe-suite

./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics
```

This starts:

- MySQL, RabbitMQ, LocalStack, Petstore mocks (Compose)
- Prometheus + Grafana (`--with-metrics`)
- dashpipe-api on port **8080**
- dashpipe-ui on port **5173**

Wait until the script prints the summary block with all URLs.

## 2. Open the UI

Visit [http://127.0.0.1:5173](http://127.0.0.1:5173).

Default tenant header used by localdev: **`T001`**.

## 3. Import a sample pipeline

1. Go to **Pipelines** in the UI.
2. Use **Import** (or create new) with a sample from [dashpipe-demo/pipelines/](../../dashpipe-demo/pipelines/).
3. Recommended first sample: `inventory-s3-to-petstore.pipeline.json`.

## 4. Verify the API

```bash
curl -s -H 'X-Tenant-Id: T001' http://localhost:8080/api/v1/pipelines | jq .
curl -s http://localhost:8080/actuator/health | jq .
```

## 5. Optional: Grafana

With `--with-metrics`, open [http://localhost:3000](http://localhost:3000) (admin / admin).

Dashboard: **Pipeline Overview** (provisioned from dashpipe-tools).

## Next steps

| Goal | Guide |
|------|-------|
| Real K8s pipelet execution | [Local development](LOCAL_DEVELOPMENT.md) |
| Full inventory E2E demo | [Use cases](../overview/USE_CASES.md) |
| AI pipeline design | [AI and MCP](AI_AND_MCP.md) |
| Deploy to Azure | [Deployment](../operations/DEPLOYMENT.md) |

## Stop the stack

```bash
./dashpipe-ci_cd/scripts/localdev.sh stop
```

Data volumes are preserved. Use `docker compose down -v` from the repo root to wipe data.
