# Local development: real pipeline runs (K8s pipelets)

Developer guide to stand up the stack, **build pipelet images**, run the API in **`local,k8s`**, and verify that **Inventory Daily → Petstore** actually executes Jobs (not the stub “completed” path).

Companion scripts: [`../../dashpipe-ci_cd/scripts/localdev.sh`](../../dashpipe-ci_cd/scripts/localdev.sh), [`../../dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh`](../../dashpipe-ci_cd/scripts/inventory-pipeline-e2e.sh).  
Sample bundle: [`dashpipe-demo/pipelines/inventory-s3-to-petstore.pipeline.json`](../../dashpipe-demo/pipelines/inventory-s3-to-petstore.pipeline.json).  
Pipelet image map: [`pipelets/REGISTRY.md`](../pipelets/REGISTRY.md).

---

## Mental model (read first)

| Mode | Spring profiles | What **Run** does |
|------|-----------------|-------------------|
| **Stub** | `local` | `StubStageWorker` advances stages and marks **completed**. No real pipelet containers. Petstore unchanged. |
| **Real** | `local,k8s` | Fabric8 creates one Job per stage in `tenant-t001`. Poller marks **completed** only after Jobs succeed (or **failed** on backoff). |

UI status **completed** alone is **not** proof pipelets ran. Always check **Jobs** and **Petstore inventory**.

```text
UI Run  →  dashpipe-api (local,k8s)
              ├─ declare RabbitMQ stage queues
              ├─ create Job exec-<id>-stage-1 … stage-5
              │     images: dashpipe/plet-*:local
              │     env: CONNECTOR_CONFIG, EXECUTION_CONFIG, AMQP_URL, IO_MODE=queue
              └─ PipeletJobStatusPoller → execution completed | failed

Petstore (:4010) ← stage-5 webhook destination
LocalStack (:4567) ← stage-1 S3 source
```

---

## Prerequisites

- **Rancher Desktop** (Docker + Kubernetes enabled)
- Java **21**, Node **20+**, `kubectl`, AWS CLI or `awslocal`
- Repo checkout at the root

```bash
docker info
kubectl get nodes
kubectl config current-context   # expect rancher-desktop
```

If `Cannot connect to the Docker daemon … ~/.rd/docker.sock`: open Rancher Desktop (or `rdctl shutdown` then reopen) and wait until Docker is ready.

Optional:

```bash
export DOCKER_HOST=unix://$HOME/.rd/docker.sock
```

---

## 1. Start localdev stack

```bash
# Compose: MySQL, RabbitMQ, LocalStack, Petstore + petstore-inventory + Prometheus/Grafana
# API: profiles local,k8s   UI: Vite with live API proxy
# Also builds dashpipe/plet-*:local images (required for Jobs)
./dashpipe-ci_cd/scripts/localdev.sh start --k8s --with-metrics
./dashpipe-ci_cd/scripts/localdev.sh status
```

Rebuild images anytime (avoids ImagePullBackOff):

```bash
./dashpipe-ci_cd/scripts/localdev.sh build-pipelets
```

Useful:

| Command | Purpose |
|---------|---------|
| `./dashpipe-ci_cd/scripts/localdev.sh logs -f` | Tail API + UI logs |
| `./dashpipe-ci_cd/scripts/localdev.sh stop` | Stop API/UI; stop Compose containers (volumes kept) |

URLs:

| Service | URL |
|---------|-----|
| UI | http://localhost:5173 |
| API | http://localhost:8080 |
| Petstore | http://localhost:4010 |
| LocalStack S3 | http://localhost:4567 |
| RabbitMQ mgmt | http://localhost:15672 (`pipeline` / `pipeline`) |
| Prometheus | http://localhost:9090 (targets must show `dashpipe-api` **UP**) |
| Grafana | http://localhost:3000 (`admin` / `admin`) → **Pipeline Overview (local)** |

If Grafana panels are empty: confirm API is up (`curl localhost:8080/actuator/prometheus | grep pipeline_`), then `./dashpipe-ci_cd/scripts/smoke-metrics.sh`. Empty scrapes usually mean Prometheus cannot reach the host API via `host.docker.internal` — recreate with `docker compose --profile metrics up -d --force-recreate prometheus`.

Confirm K8s mode in API log: active profiles include `k8s`, and `stub-stage-worker` is false (`application-k8s.yml`).

---

## 2. Build and install pipelet images

Jobs use `imagePullPolicy: IfNotPresent` and tags `dashpipe/<pipeletId>:local`. Images must exist in the **same** Docker store Rancher uses.

```bash
for p in plet-s3-source plet-csv-source plet-rest-source plet-csv-to-json plet-python-filter plet-field-mapper plet-webhook-destination; do
  rel=$(python3 -c "import json; print(json.load(open('pipelets/PATHS.json'))['$p'])")
  docker build -f "pipelets/$rel/Dockerfile" -t "dashpipe/${p}:local" pipelets
done

docker images 'dashpipe/plet-*' --format '{{.Repository}}:{{.Tag}}'
```

Rebuild whenever you change `pipelets/_common/` (queue I/O) or a pipelet’s `main.py`.

---

## 3. Seed S3 input

`localdev.sh` seeds this by default when `aws` or `awslocal` is on `PATH`. Re-run anytime:

```bash
# Option A — AWS CLI against LocalStack (no awslocal required)
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1
aws --endpoint-url=http://localhost:4567 s3 mb s3://demo-s3-source 2>/dev/null || true
aws --endpoint-url=http://localhost:4567 s3 cp dashpipe-demo/petstore/samples/inventory.csv \
  s3://demo-s3-source/inventory/daily.csv
aws --endpoint-url=http://localhost:4567 s3 ls s3://demo-s3-source/inventory/

# Option B — awslocal (optional: pip install awscli-local)
# awslocal s3 mb s3://demo-s3-source 2>/dev/null || true
# awslocal s3 cp dashpipe-demo/petstore/samples/inventory.csv s3://demo-s3-source/inventory/daily.csv
```

---

## 4. Import / open pipeline in UI

1. Open http://localhost:5173 — tenant **T001**
2. **Pipelines → Import** → `dashpipe-demo/pipelines/inventory-s3-to-petstore.pipeline.json`  
   (or open an already-imported **Inventory Daily to Petstore**)
3. Confirm connector endpoints use **`host.docker.internal`** (not `localhost`):
   - S3: `http://host.docker.internal:4567`
   - Petstore: `http://host.docker.internal:4010/api/v3`
4. **Save** → **Deploy** (leave DRAFT)
5. Open the **Debug / logs** panel on the builder (select a run) for live execution detail

---

## 5. Baseline Petstore, then Run

```bash
curl -s http://localhost:4010/api/v3/inventory/summary | python3 -m json.tool
```

In the builder: **Run**. Copy `executionId` from the URL (`?executionId=…`) or Run history.

---

## 6. End-to-end verification checklist

### A. Kubernetes Jobs (must appear)

```bash
kubectl get jobs,pods -n tenant-t001
# Watch:
kubectl get jobs,pods -n tenant-t001 -w
```

Expect Jobs named like `exec-<executionId>-stage-1` … `stage-5`.

```bash
EID=<execution-id>
kubectl logs -n tenant-t001 -l dashpipe.io/execution_id=$EID,dashpipe.io/stage_order=1 --tail=80
kubectl logs -n tenant-t001 -l dashpipe.io/execution_id=$EID,dashpipe.io/stage_order=5 --tail=80
```

### B. Execution API

```bash
PID=<pipeline-id>
EID=<execution-id>
curl -sf -H "X-Tenant-Id: T001" \
  "http://localhost:8080/api/v1/pipelines/$PID/executions/$EID" | python3 -m json.tool

curl -sf -H "X-Tenant-Id: T001" \
  "http://localhost:8080/api/v1/observability/executions/$EID/logs" | python3 -m json.tool
```

### C. Petstore data changed (hard proof)

```bash
curl -s http://localhost:4010/api/v3/inventory/summary | python3 -m json.tool
curl -s http://localhost:4010/api/v3/inventory/items | python3 -m json.tool | head -80
```

Expect non-zero inventory after a successful real run.

### D. Metrics (optional)

```bash
curl -s http://localhost:8080/actuator/prometheus | grep -E 'pipelet_records|pipeline_completeness'
# Grafana → Pipeline Overview (local)
```

### E. Stub smell test

If Run finishes in a few seconds, **no Jobs** in `tenant-t001`, and Petstore is unchanged → API is still on **stub** (`local` only). Fix:

```bash
./dashpipe-ci_cd/scripts/localdev.sh stop
./dashpipe-ci_cd/scripts/localdev.sh start --k8s --with-metrics
```

---

## 7. Failure triage

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| ImagePullBackOff | Image missing in Rancher store | Execution auto-fails with `Image pull failed…` + `build-pipelets` hint. Rebuild: `./dashpipe-ci_cd/scripts/localdev.sh build-pipelets` |
| Status stuck `running` with blank `k8s_POD_*` logs | Pause container / Pending pull | Check `-c pipelet`; API now fails early on ImagePullBackOff instead of hanging |
| BackoffLimitExceeded | Pipelet crash | `kubectl logs` for that stage; check connector JSON |
| Pod can’t reach S3/Petstore | Endpoint is `localhost` inside the pod | Use `host.docker.internal` on connectors |
| Pod can’t reach RabbitMQ | Wrong AMQP host | Use profile `local,k8s` (`amqp-url` → `host.docker.internal:5672`) |
| Completed + empty Petstore | Stub mode **or** stale RabbitMQ messages | Restart with `--k8s`; new runs purge stage queues automatically |
| Stage-2 `records=0` / kickoff `payload=run-…` treated as CSV | Leftover stage messages from prior stub runs | Fixed: purge on run + CSV pipelet ignores `run-*` payloads; rebuild `plet-csv-to-json` image |
| Stage-5 empty items / JSON SyntaxError | Downstream of empty stage-2 | Rebuild webhook image; re-run after purge |
| Docker sock errors | Rancher VM / socket stale | Restart Rancher Desktop |

More detail: [`docs/delivery/kb/W2-US05-pipelet-job.md`](delivery/kb/W2-US05-pipelet-job.md).

---

## 8. UI debug panel

On the pipeline builder, with a selected execution:

- **Status / records / completeness / error summary**
- **Indexed execution logs** (`GET /api/v1/observability/executions/{id}/logs`)
- **Dev commands** (kubectl, Petstore curl) you can copy

Observability page also lists run history and links to Grafana when `PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL` is set (`localdev.sh --with-metrics` sets it).

---

Observability page also lists run history and links to Grafana when `PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL` is set (`localdev.sh --with-metrics` sets it).

---

## MCP (Cursor · Claude · OpenClaw · GitHub Copilot)

The [`dashpipe-dev-ai-agent/dashpipe-mcp/`](../../dashpipe-dev-ai-agent/dashpipe-mcp/) package exposes dashpipe-api as MCP tools for Cursor, Claude Desktop, OpenClaw, and VS Code / GitHub Copilot. See [dashpipe-mcp/README.md](../../dashpipe-dev-ai-agent/dashpipe-mcp/README.md).

```bash
cd dashpipe-dev-ai-agent/dashpipe-mcp && python3 -m venv .venv && source .venv/bin/activate
pip install -e .
python -m dashpipe_mcp
```

See [`dashpipe-dev-ai-agent/dashpipe-mcp/README.md`](../../dashpipe-dev-ai-agent/dashpipe-mcp/README.md).

---

## Quick command card

```bash
./dashpipe-ci_cd/scripts/localdev.sh start --k8s --with-metrics
for p in plet-s3-source plet-csv-source plet-rest-source plet-csv-to-json plet-python-filter plet-field-mapper plet-webhook-destination; do
  rel=$(python3 -c "import json; print(json.load(open('pipelets/PATHS.json'))['$p'])")
  docker build -f "pipelets/$rel/Dockerfile" -t "dashpipe/${p}:local" pipelets
done
# UI: import sample → Deploy → Run
kubectl get jobs,pods -n tenant-t001 -w
curl -s http://localhost:4010/api/v3/inventory/summary | python3 -m json.tool
```
