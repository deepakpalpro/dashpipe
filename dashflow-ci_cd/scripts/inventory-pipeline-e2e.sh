#!/usr/bin/env bash
# End-to-end: inventory CSV in LocalStack S3 → pipelet(s) → Petstore upload.
#
# Usage:
#   ./scripts/inventory-pipeline-e2e.sh           # docker run (default)
#   ./scripts/inventory-pipeline-e2e.sh --k8s     # Kind / current kubectl context
#   ./scripts/inventory-pipeline-e2e.sh --stages  # chained stage pipelets via docker
#   ./scripts/inventory-pipeline-e2e.sh --register-only  # API pipeline + connectors only
#
set -euo pipefail

SUITE="$(cd "$(dirname "$0")/../.." && pwd)"
PLATFORM="$SUITE/dashflow-platform"
DEMO="$SUITE/dashflow-demo"
CICD="$SUITE/dashflow-ci_cd"
CSV_FILE="${CSV_FILE:-$DEMO/petstore/samples/inventory.csv}"
IMAGE="${IMAGE:-dashflow/inventory-pipelet:local}"
BUCKET="${S3_BUCKET:-demo-s3-source}"
OBJECT_KEY="${S3_OBJECT_KEY:-inventory/daily.csv}"
S3_ENDPOINT="${S3_ENDPOINT:-http://localhost:4567}"
PETSTORE_URL="${PETSTORE_URL:-http://localhost:4010}"
API_URL="${API_URL:-http://localhost:8080}"
TENANT_ID="${TENANT_ID:-T001}"
MODE="docker"
REGISTER_ONLY=0

for arg in "$@"; do
  case "$arg" in
    --k8s) MODE="k8s" ;;
    --stages) MODE="stages" ;;
    --register-only) REGISTER_ONLY=1 ;;
    -h|--help)
      sed -n '2,12p' "$0"
      exit 0
      ;;
  esac
done

log() { printf '==> %s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

need() { command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"; }

aws_local() {
  if command -v awslocal >/dev/null 2>&1; then
    awslocal "$@"
  elif command -v aws >/dev/null 2>&1; then
    AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1 \
      aws --endpoint-url="$S3_ENDPOINT" "$@"
  else
    # Python fallback via boto3 in a throwaway container after image build
    docker run --rm --network host \
      -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test \
      -e S3_ENDPOINT="$S3_ENDPOINT" -e BUCKET="$BUCKET" -e KEY="$OBJECT_KEY" \
      -v "$CSV_FILE:/data/daily.csv:ro" \
      "$IMAGE" python - <<'PY'
import os, boto3
from botocore.client import Config
c = boto3.client("s3", endpoint_url=os.environ["S3_ENDPOINT"], region_name="us-east-1",
  aws_access_key_id="test", aws_secret_access_key="test",
  config=Config(s3={"addressing_style":"path"}))
b, k = os.environ["BUCKET"], os.environ["KEY"]
try: c.head_bucket(Bucket=b)
except Exception: c.create_bucket(Bucket=b)
c.upload_file("/data/daily.csv", b, k)
print(f"uploaded s3://{b}/{k}")
PY
    return
  fi
}

ensure_deps() {
  log "Ensuring Docker Compose deps (mysql, rabbitmq, localstack, petstore)"
  # Free host :4010 if a local npm petstore is bound (compose needs the port)
  if lsof -nP -iTCP:4010 -sTCP:LISTEN >/dev/null 2>&1; then
    if docker ps --format '{{.Names}}' | grep -qx df-petstore; then
      :
    else
      log "Stopping host process on :4010 so Compose petstore can bind"
      if [[ -f "$SUITE/.petstore-e2e.pid" ]]; then
        kill "$(cat "$SUITE/.petstore-e2e.pid")" 2>/dev/null || true
        rm -f "$SUITE/.petstore-e2e.pid"
      fi
      # Best-effort: kill listeners on 4010 (macOS xargs has no -r)
      pids="$(lsof -tiTCP:4010 -sTCP:LISTEN 2>/dev/null || true)"
      if [[ -n "$pids" ]]; then
        # shellcheck disable=SC2086
        kill $pids 2>/dev/null || true
      fi
      sleep 1
    fi
  fi
  (cd "$SUITE" && docker compose --profile petstore up -d --build mysql rabbitmq localstack petstore)
  for i in $(seq 1 60); do
    if curl -sf "$S3_ENDPOINT/_localstack/health" >/dev/null 2>&1 \
      && curl -sf "$PETSTORE_URL/health" >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done
  curl -sf "$S3_ENDPOINT/_localstack/health" >/dev/null || die "LocalStack not healthy at $S3_ENDPOINT"
  curl -sf "$PETSTORE_URL/health" >/dev/null || die "Petstore not healthy at $PETSTORE_URL"
}

ensure_petstore() {
  # Petstore is started via Compose in ensure_deps
  log "Petstore via Compose at $PETSTORE_URL"
}

seed_s3() {
  log "Seeding s3://$BUCKET/$OBJECT_KEY from $CSV_FILE"
  [[ -f "$CSV_FILE" ]] || die "CSV not found: $CSV_FILE"
  if command -v awslocal >/dev/null 2>&1 || command -v aws >/dev/null 2>&1; then
    aws_local s3 mb "s3://$BUCKET" 2>/dev/null || true
    aws_local s3 cp "$CSV_FILE" "s3://$BUCKET/$OBJECT_KEY"
  else
    need docker
    build_image
    docker run --rm --network host \
      -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test \
      -v "$CSV_FILE:/data/daily.csv:ro" \
      --entrypoint python "$IMAGE" - <<PY
import boto3
from botocore.client import Config
c = boto3.client("s3", endpoint_url="$S3_ENDPOINT", region_name="us-east-1",
  aws_access_key_id="test", aws_secret_access_key="test",
  config=Config(s3={"addressing_style":"path"}))
try: c.head_bucket(Bucket="$BUCKET")
except Exception: c.create_bucket(Bucket="$BUCKET")
c.upload_file("/data/daily.csv", "$BUCKET", "$OBJECT_KEY")
print("uploaded s3://$BUCKET/$OBJECT_KEY")
PY
  fi
}

build_image() {
  log "Building pipelet image $IMAGE"
  docker build -t "$IMAGE" "$PLATFORM/pipelets/inventory"
}

register_api() {
  if ! curl -sf -H "X-Tenant-Id: $TENANT_ID" "$API_URL/actuator/health" >/dev/null 2>&1; then
    log "API not reachable at $API_URL — skipping pipeline registration"
    return 0
  fi
  log "Registering connectors + pipeline on API ($API_URL)"
  python3 - <<PY
import json, os, urllib.request

API = os.environ.get("API_URL", "$API_URL")
TENANT = os.environ.get("TENANT_ID", "$TENANT_ID")

def req(method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    r = urllib.request.Request(
        API + path, data=data, method=method,
        headers={"Content-Type": "application/json", "X-Tenant-Id": TENANT, "Accept": "application/json"},
    )
    with urllib.request.urlopen(r) as resp:
        raw = resp.read().decode()
        return json.loads(raw) if raw else None

# Storage connector for inventory CSV
try:
    storage = req("POST", "/api/v1/connectors", {
        "connectorTypeId": "ct-storage",
        "name": "Inventory S3 Source",
        "config": {
            "bucket": "$BUCKET",
            "endpoint": "http://localhost:4567",
            "region": "us-east-1",
            "accessKeyId": "test",
            "secretAccessKey": "test",
            "createBucketIfMissing": True,
        },
        "execution_config": {"objectKey": "$OBJECT_KEY", "prefix": "inventory/"},
    })
    storage_id = storage["id"]
    print("storage connector", storage_id)
except Exception as e:
    print("storage connector create skipped/failed:", e)
    storage_id = "conn-plet-s3-source"

# REST connector for Petstore
try:
    rest = req("POST", "/api/v1/connectors", {
        "connectorTypeId": "ct-rest",
        "name": "Petstore Inventory API",
        "config": {
            "baseUrl": "http://localhost:4010/api/v3",
            "timeoutMs": 30000,
            "pingPath": "/health",
        },
        "execution_config": {"path": "/inventory/upload", "method": "POST"},
    })
    rest_id = rest["id"]
    print("rest connector", rest_id)
except Exception as e:
    print("rest connector create skipped/failed:", e)
    rest_id = None

# Pipeline
try:
    pipe = req("POST", "/api/v1/pipelines", {
        "name": "Inventory Daily to Petstore",
        "description": "S3 CSV ingest -> parse -> filter -> map -> Petstore upload",
    })
    pid = pipe["id"]
    print("pipeline", pid)
    steps = {
        "steps": [
            {
                "pipelet_id": "plet-s3-source",
                "step_order": 1,
                "connector_ids": [storage_id],
                "execution_config": {"bucket": "$BUCKET", "objectKey": "$OBJECT_KEY"},
            },
            {
                "pipelet_id": "plet-csv-to-json",
                "step_order": 2,
                "execution_config": {},
            },
            {
                "pipelet_id": "plet-python-filter",
                "step_order": 3,
                "execution_config": {
                    "categories": ["food", "accessories", "toys"],
                    "minQuantity": 1,
                },
            },
            {
                "pipelet_id": "plet-field-mapper",
                "step_order": 4,
                "execution_config": {"mode": "upsert", "target": "petstore"},
            },
            {
                "pipelet_id": "plet-webhook-destination",
                "step_order": 5,
                "connector_ids": [rest_id] if rest_id else [],
                "execution_config": {
                    "path": "/inventory/upload",
                    "method": "POST",
                    "baseUrl": "http://localhost:4010/api/v3",
                },
            },
        ]
    }
    req("PUT", f"/api/v1/pipelines/{pid}/steps", steps)
    print("steps registered")
    with open("$SUITE/.inventory-pipeline-id", "w") as f:
        f.write(pid)
except Exception as e:
    print("pipeline register skipped/failed:", e)
PY
}

run_docker_batch() {
  build_image
  log "Running batch pipelet via Docker (compose network)"
  local net
  net="$(docker inspect df-localstack -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}' | head -1)"
  [[ -n "$net" ]] || die "Could not resolve compose network from df-localstack"
  docker run --rm --network "$net" \
    -e RUN_MODE=batch \
    -e PIPELET_ID=inventory-batch \
    -e S3_CONNECTOR_CONFIG="$(cat <<JSON
{"bucket":"$BUCKET","endpoint":"http://localstack:4566","region":"us-east-1","accessKeyId":"test","secretAccessKey":"test"}
JSON
)" \
    -e REST_CONNECTOR_CONFIG="$(cat <<JSON
{"baseUrl":"http://petstore:4010/api/v3","pingPath":"/health"}
JSON
)" \
    -e EXECUTION_CONFIG="$(cat <<JSON
{"bucket":"$BUCKET","objectKey":"$OBJECT_KEY","mode":"upsert","path":"/inventory/upload"}
JSON
)" \
    "$IMAGE"
}

run_stages() {
  build_image
  log "Running chained stage pipelets on compose network"
  local net tmp
  net="$(docker inspect df-localstack -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}' | head -1)"
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' RETURN

  docker run --rm -i --network "$net" \
    -e RUN_MODE=stage -e PIPELET_ID=plet-s3-source \
    -e CONNECTOR_CONFIG="{\"bucket\":\"$BUCKET\",\"endpoint\":\"http://localstack:4566\",\"region\":\"us-east-1\",\"accessKeyId\":\"test\",\"secretAccessKey\":\"test\"}" \
    -e EXECUTION_CONFIG="{\"bucket\":\"$BUCKET\",\"objectKey\":\"$OBJECT_KEY\"}" \
    "$IMAGE" < /dev/null >"$tmp/1.json"

  docker run --rm -i --network "$net" \
    -e RUN_MODE=stage -e PIPELET_ID=plet-csv-to-json \
    "$IMAGE" <"$tmp/1.json" >"$tmp/2.json"

  docker run --rm -i --network "$net" \
    -e RUN_MODE=stage -e PIPELET_ID=plet-python-filter \
    "$IMAGE" <"$tmp/2.json" >"$tmp/3.json"

  docker run --rm -i --network "$net" \
    -e RUN_MODE=stage -e PIPELET_ID=plet-field-mapper \
    -e EXECUTION_CONFIG='{"mode":"upsert"}' \
    "$IMAGE" <"$tmp/3.json" >"$tmp/4.json"

  docker run --rm -i --network "$net" \
    -e RUN_MODE=stage -e PIPELET_ID=plet-webhook-destination \
    -e CONNECTOR_CONFIG='{"baseUrl":"http://petstore:4010/api/v3"}' \
    -e EXECUTION_CONFIG='{"path":"/inventory/upload"}' \
    "$IMAGE" <"$tmp/4.json" | tee "$tmp/5.json"
}

run_k8s() {
  need kubectl
  build_image
  if command -v kind >/dev/null 2>&1; then
    if ! kind get clusters 2>/dev/null | grep -qx 'dashflow'; then
      log "Creating Kind cluster dashflow"
      kind create cluster --name dashflow
    fi
    log "Loading image into Kind"
    kind load docker-image "$IMAGE" --name dashflow
  else
    log "kind not found — using current kubectl context (image must be pullable)"
  fi

  # Patch endpoint for in-cluster: use host.docker.internal (Docker Desktop / Kind)
  kubectl apply -f "$CICD/k8s/inventory/inventory-pipeline.yaml"
  kubectl -n tenant-t001 delete job inventory-s3-to-petstore --ignore-not-found
  kubectl apply -f "$CICD/k8s/inventory/inventory-pipeline.yaml"
  log "Waiting for Job to complete"
  kubectl -n tenant-t001 wait --for=condition=complete job/inventory-s3-to-petstore --timeout=180s \
    || {
      kubectl -n tenant-t001 logs job/inventory-s3-to-petstore || true
      die "K8s Job failed"
    }
  kubectl -n tenant-t001 logs job/inventory-s3-to-petstore
}

verify_petstore() {
  log "Verifying Petstore inventory"
  local summary
  summary="$(curl -sf "$PETSTORE_URL/api/v3/inventory/summary")"
  echo "$summary" | python3 -m json.tool 2>/dev/null || echo "$summary"
  local count
  count="$(curl -sf "$PETSTORE_URL/api/v3/inventory/items" | python3 -c 'import sys,json; print(len(json.load(sys.stdin)))')"
  [[ "$count" -ge 1 ]] || die "Expected inventory items after upload, got $count"
  log "OK — $count inventory items present"
}

# --- main ---
need docker
need curl
ensure_deps
ensure_petstore
seed_s3
register_api

if [[ "$REGISTER_ONLY" -eq 1 ]]; then
  log "Register-only done"
  exit 0
fi

case "$MODE" in
  docker) run_docker_batch ;;
  stages) run_stages ;;
  k8s) run_k8s ;;
esac

verify_petstore
log "Inventory pipeline e2e succeeded ($MODE)"
