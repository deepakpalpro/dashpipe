#!/usr/bin/env bash
# Bring up the full local e2e stack: Compose deps + Petstore + API + UI.
#
# Usage:
#   ./scripts/localdev.sh                 # start (default)
#   ./scripts/localdev.sh start           # same
#   ./scripts/localdev.sh start --with-metrics --with-elk
#   ./scripts/localdev.sh start --k8s     # API profiles local,k8s + build pipelet images
#   ./scripts/localdev.sh build-pipelets  # build dashflow/plet-*:local images only
#   ./scripts/localdev.sh stop
#   ./scripts/localdev.sh status
#   ./scripts/localdev.sh logs            # tail API + UI logs
#
# Guide: docs/LOCALDEV_PIPELINE_GUIDE.md
#
# Env overrides:
#   API_URL, UI_URL, PETSTORE_URL, PETSTORE_INVENTORY_URL, S3_ENDPOINT, TENANT_ID
#   PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL (default http://localhost:3000 when --with-metrics)
#   PIPELET_IDS  space-separated pipelet dirs under pipelets/ (default: catalog below)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STATE_DIR="$ROOT/.localdev"
API_PID_FILE="$STATE_DIR/api.pid"
UI_PID_FILE="$STATE_DIR/ui.pid"
API_LOG="$STATE_DIR/api.log"
UI_LOG="$STATE_DIR/ui.log"

API_URL="${API_URL:-http://localhost:8080}"
UI_URL="${UI_URL:-http://localhost:5173}"
PETSTORE_URL="${PETSTORE_URL:-http://localhost:4010}"
PETSTORE_INVENTORY_URL="${PETSTORE_INVENTORY_URL:-http://localhost:4011}"
S3_ENDPOINT="${S3_ENDPOINT:-http://localhost:4567}"
TENANT_ID="${TENANT_ID:-T001}"
CSV_FILE="${CSV_FILE:-$ROOT/mockservice/petstore/samples/inventory.csv}"
S3_BUCKET="${S3_BUCKET:-demo-s3-source}"
S3_OBJECT_KEY="${S3_OBJECT_KEY:-inventory/daily.csv}"

CMD="start"
WITH_METRICS=0
WITH_ELK=0
WITH_K8S=0
BUILD_PIPELETS=0
NO_API=0
NO_UI=0
SEED_S3=1
FOLLOW=0

# Catalog pipelets with Dockerfiles (must exist before K8s Jobs / ImagePullBackOff).
DEFAULT_PIPELET_IDS=(
  plet-s3-source
  plet-csv-source
  plet-rest-source
  plet-csv-to-json
  plet-python-filter
  plet-field-mapper
  plet-webhook-destination
)

usage() {
  sed -n '2,20p' "$0"
}

log() { printf '==> %s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"; }

parse_args() {
  if [[ $# -gt 0 ]]; then
    case "$1" in
      start|stop|status|logs|build-pipelets|help|-h|--help)
        CMD="$1"
        shift
        ;;
    esac
  fi
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --with-metrics) WITH_METRICS=1 ;;
      --with-elk) WITH_ELK=1 ;;
      --k8s) WITH_K8S=1; BUILD_PIPELETS=1 ;;
      --build-pipelets) BUILD_PIPELETS=1 ;;
      --no-build-pipelets) BUILD_PIPELETS=0 ;;
      --no-api) NO_API=1 ;;
      --no-ui) NO_UI=1 ;;
      --no-seed) SEED_S3=0 ;;
      --seed-s3) SEED_S3=1 ;;
      -f|--follow) FOLLOW=1 ;;
      -h|--help|help) usage; exit 0 ;;
      *) die "Unknown argument: $1 (try --help)" ;;
    esac
    shift
  done
  if [[ "$CMD" == "help" || "$CMD" == "-h" || "$CMD" == "--help" ]]; then
    usage
    exit 0
  fi
}

ensure_docker() {
  need docker
  if ! docker info >/dev/null 2>&1; then
    die "Docker daemon not reachable (Rancher Desktop running? try: open -a 'Rancher Desktop')"
  fi
  if [[ -z "${DOCKER_HOST:-}" && -S "$HOME/.rd/docker.sock" ]]; then
    export DOCKER_HOST="unix://$HOME/.rd/docker.sock"
  fi
}

pipelet_ids() {
  if [[ -n "${PIPELET_IDS:-}" ]]; then
    # shellcheck disable=SC2206
    local ids=(${PIPELET_IDS})
    printf '%s\n' "${ids[@]}"
    return
  fi
  printf '%s\n' "${DEFAULT_PIPELET_IDS[@]}"
}

build_pipelets() {
  ensure_docker
  local id dockerfile failed=0
  log "Building pipelet images (tag dashflow/<id>:local) into Docker store used by Rancher"
  while IFS= read -r id; do
    [[ -n "$id" ]] || continue
    dockerfile="$ROOT/pipelets/$id/Dockerfile"
    if [[ ! -f "$dockerfile" ]]; then
      log "skip $id (no Dockerfile)"
      continue
    fi
    log "docker build $id"
    if ! docker build -f "$dockerfile" -t "dashflow/${id}:local" "$ROOT/pipelets"; then
      printf 'ERROR: failed to build dashflow/%s:local\n' "$id" >&2
      failed=1
    fi
  done < <(pipelet_ids)
  if [[ "$failed" -ne 0 ]]; then
    die "One or more pipelet images failed to build"
  fi
  log "Pipelet images ready:"
  docker images 'dashflow/plet-*' --format '  {{.Repository}}:{{.Tag}}' 2>/dev/null || true
}

compose_profiles() {
  local args=(--profile petstore)
  if [[ "$WITH_METRICS" -eq 1 ]]; then
    args+=(--profile metrics)
  fi
  if [[ "$WITH_ELK" -eq 1 ]]; then
    args+=(--profile elk)
  fi
  printf '%s\n' "${args[@]}"
}

wait_http() {
  local url="$1" name="$2" attempts="${3:-60}"
  local i
  for i in $(seq 1 "$attempts"); do
    if curl -sf "$url" >/dev/null 2>&1; then
      log "$name ready ($url)"
      return 0
    fi
    sleep 1
  done
  die "$name not healthy after ${attempts}s: $url"
}

pid_alive() {
  local pid="$1"
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

read_pid() {
  local file="$1"
  [[ -f "$file" ]] || { echo ""; return; }
  tr -d '[:space:]' <"$file"
}

stop_pid_file() {
  local file="$1" name="$2"
  local pid
  pid="$(read_pid "$file")"
  if pid_alive "$pid"; then
    log "Stopping $name (pid $pid)"
    kill "$pid" 2>/dev/null || true
    local i
    for i in $(seq 1 20); do
      pid_alive "$pid" || break
      sleep 0.5
    done
    if pid_alive "$pid"; then
      kill -9 "$pid" 2>/dev/null || true
    fi
  fi
  rm -f "$file"
}

free_port_if_ours() {
  # Best-effort: if something other than our tracked process holds the port, warn.
  local port="$1"
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    log "Port $port already in use"
  fi
}

start_compose() {
  local label="petstore"
  [[ "$WITH_METRICS" -eq 1 ]] && label="${label}+metrics"
  [[ "$WITH_ELK" -eq 1 ]] && label="${label}+elk"
  log "Starting Docker Compose ($label)"

  local profiles=()
  while IFS= read -r p; do
    [[ -n "$p" ]] && profiles+=("$p")
  done < <(compose_profiles)

  local services=(mysql rabbitmq localstack petstore petstore-inventory)
  if [[ "$WITH_METRICS" -eq 1 ]]; then
    services+=(prometheus grafana)
  fi
  if [[ "$WITH_ELK" -eq 1 ]]; then
    services+=(elasticsearch kibana logstash)
  fi

  (cd "$ROOT" && docker compose "${profiles[@]}" up -d --build "${services[@]}")

  wait_http "$S3_ENDPOINT/_localstack/health" "LocalStack" 90
  wait_http "$PETSTORE_URL/health" "Petstore" 90
  wait_http "$PETSTORE_INVENTORY_URL/health" "Petstore-Inventory" 60

  if [[ "$WITH_METRICS" -eq 1 ]]; then
    wait_http "http://127.0.0.1:9090/-/healthy" "Prometheus" 60
    wait_http "http://127.0.0.1:3000/api/health" "Grafana" 60
  fi
  if [[ "$WITH_ELK" -eq 1 ]]; then
    wait_http "http://127.0.0.1:9200/_cluster/health" "Elasticsearch" 120
  fi
}

seed_s3() {
  [[ "$SEED_S3" -eq 1 ]] || return 0
  [[ -f "$CSV_FILE" ]] || die "CSV not found: $CSV_FILE"
  log "Seeding s3://$S3_BUCKET/$S3_OBJECT_KEY"
  if command -v awslocal >/dev/null 2>&1; then
    awslocal s3 mb "s3://$S3_BUCKET" 2>/dev/null || true
    awslocal s3 cp "$CSV_FILE" "s3://$S3_BUCKET/$S3_OBJECT_KEY"
  elif command -v aws >/dev/null 2>&1; then
    AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1 \
      aws --endpoint-url="$S3_ENDPOINT" s3 mb "s3://$S3_BUCKET" 2>/dev/null || true
    AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1 \
      aws --endpoint-url="$S3_ENDPOINT" s3 cp "$CSV_FILE" "s3://$S3_BUCKET/$S3_OBJECT_KEY"
  else
    log "No aws/awslocal — skip S3 seed (install AWS CLI or run inventory-pipeline-e2e.sh)"
  fi
}

start_api() {
  [[ "$NO_API" -eq 0 ]] || return 0
  need java
  mkdir -p "$STATE_DIR"

  local existing
  existing="$(read_pid "$API_PID_FILE")"
  if pid_alive "$existing"; then
    log "API already running (pid $existing)"
    return 0
  fi
  if curl -sf "$API_URL/actuator/health" >/dev/null 2>&1; then
    log "API already healthy at $API_URL (not started by this script)"
    return 0
  fi

  free_port_if_ours 8080
  local profiles="local"
  if [[ "$WITH_K8S" -eq 1 ]]; then
    profiles="local,k8s"
  fi

  export PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL="${PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL:-}"
  if [[ "$WITH_METRICS" -eq 1 && -z "${PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL}" ]]; then
    export PIPELINE_OBSERVABILITY_GRAFANA_BASE_URL="http://localhost:3000"
  fi
  if [[ "$WITH_ELK" -eq 1 && -z "${PIPELINE_OBSERVABILITY_ELASTICSEARCH_BASE_URL:-}" ]]; then
    export PIPELINE_OBSERVABILITY_ELASTICSEARCH_BASE_URL="http://localhost:9200"
  fi

  log "Starting dashflow-api (profiles=$profiles) → $API_LOG"
  (
    cd "$ROOT"
    # Prefer DOCKER_HOST for Rancher if unset
    if [[ -z "${DOCKER_HOST:-}" && -S "$HOME/.rd/docker.sock" ]]; then
      export DOCKER_HOST="unix://$HOME/.rd/docker.sock"
    fi
    nohup bash -c '
      set -euo pipefail
      ./mvnw -pl dashflow-api -am install -DskipTests -q
      exec ./mvnw -pl dashflow-api spring-boot:run -Dspring-boot.run.profiles="'"$profiles"'"
    ' >"$API_LOG" 2>&1 &
    echo $! >"$API_PID_FILE"
  )
  wait_http "$API_URL/actuator/health" "API" 180
}

start_ui() {
  [[ "$NO_UI" -eq 0 ]] || return 0
  need npm
  mkdir -p "$STATE_DIR"

  local existing
  existing="$(read_pid "$UI_PID_FILE")"
  if pid_alive "$existing"; then
    log "UI already running (pid $existing)"
    return 0
  fi
  if curl -sf "$UI_URL" >/dev/null 2>&1; then
    log "UI already responding at $UI_URL (not started by this script)"
    return 0
  fi

  if [[ ! -d "$ROOT/dashflow-ui/node_modules" ]]; then
    log "Installing dashflow-ui dependencies"
    (cd "$ROOT/dashflow-ui" && npm install)
  fi

  free_port_if_ours 5173
  log "Starting dashflow-ui (dev:api) → $UI_LOG"
  (
    cd "$ROOT/dashflow-ui"
    nohup npm run dev:api >"$UI_LOG" 2>&1 &
    echo $! >"$UI_PID_FILE"
  )
  wait_http "$UI_URL" "UI" 90
}

print_summary() {
  cat <<EOF

────────────────────────────────────────
 Local e2e stack is up
────────────────────────────────────────
  UI         $UI_URL
  API        $API_URL
  Petstore   $PETSTORE_URL
  Inventory  $PETSTORE_INVENTORY_URL  (GET /api/v3/inventory/items)
  LocalStack $S3_ENDPOINT
  RabbitMQ   http://localhost:15672  (pipeline / pipeline)
EOF
  if [[ "$WITH_METRICS" -eq 1 ]]; then
    cat <<EOF
  Prometheus http://localhost:9090
  Grafana    http://localhost:3000  (admin / admin)
EOF
  fi
  if [[ "$WITH_ELK" -eq 1 ]]; then
    cat <<EOF
  Elasticsearch http://localhost:9200
  Kibana        http://localhost:5601
EOF
  fi
  cat <<EOF

  Tenant header: X-Tenant-Id: $TENANT_ID
  Logs:          $STATE_DIR/
  Stop:          ./scripts/localdev.sh stop
  Status:        ./scripts/localdev.sh status
  Pipelets:      ./scripts/localdev.sh build-pipelets
────────────────────────────────────────
EOF
}

do_start() {
  ensure_docker
  mkdir -p "$STATE_DIR"
  start_compose
  seed_s3
  if [[ "$BUILD_PIPELETS" -eq 1 ]]; then
    build_pipelets
  fi
  start_api
  start_ui
  print_summary
}

do_stop() {
  stop_pid_file "$UI_PID_FILE" "UI"
  stop_pid_file "$API_PID_FILE" "API"

  if docker info >/dev/null 2>&1; then
    log "Stopping Compose services (keeping volumes)"
    (
      cd "$ROOT"
      docker compose --profile petstore --profile metrics --profile elk stop \
        mysql rabbitmq localstack petstore petstore-inventory prometheus grafana elasticsearch kibana logstash \
        2>/dev/null || docker compose stop 2>/dev/null || true
    )
  else
    log "Docker not reachable — skipped Compose stop"
  fi
  log "Stopped. Data volumes retained (docker compose down -v to wipe)."
}

do_status() {
  local ok=0
  check() {
    local name="$1" url="$2"
    if curl -sf "$url" >/dev/null 2>&1; then
      printf '  OK   %-12s %s\n' "$name" "$url"
    else
      printf '  DOWN %-12s %s\n' "$name" "$url"
      ok=1
    fi
  }
  echo "Service status:"
  check API "$API_URL/actuator/health"
  check UI "$UI_URL"
  check Petstore "$PETSTORE_URL/health"
  check Inventory "$PETSTORE_INVENTORY_URL/health"
  check LocalStack "$S3_ENDPOINT/_localstack/health"
  check RabbitMQ "http://localhost:15672"
  if curl -sf "http://127.0.0.1:9090/-/healthy" >/dev/null 2>&1; then
    check Prometheus "http://127.0.0.1:9090/-/healthy"
  fi
  if curl -sf "http://127.0.0.1:3000/api/health" >/dev/null 2>&1; then
    check Grafana "http://127.0.0.1:3000/api/health"
  fi

  local apid uid
  apid="$(read_pid "$API_PID_FILE")"
  uid="$(read_pid "$UI_PID_FILE")"
  echo "Tracked processes:"
  if pid_alive "$apid"; then echo "  API pid $apid"; else echo "  API pid (none / not ours)"; fi
  if pid_alive "$uid"; then echo "  UI  pid $uid"; else echo "  UI  pid (none / not ours)"; fi
  return "$ok"
}

do_logs() {
  mkdir -p "$STATE_DIR"
  touch "$API_LOG" "$UI_LOG"
  if [[ "$FOLLOW" -eq 1 ]]; then
    tail -n 50 -F "$API_LOG" "$UI_LOG"
  else
    echo "---- API ($API_LOG) ----"
    tail -n 40 "$API_LOG" 2>/dev/null || true
    echo "---- UI ($UI_LOG) ----"
    tail -n 40 "$UI_LOG" 2>/dev/null || true
    echo "(use: ./scripts/localdev.sh logs -f)"
  fi
}

parse_args "$@"
case "$CMD" in
  start) do_start ;;
  stop) do_stop ;;
  status) do_status ;;
  logs) do_logs ;;
  build-pipelets) build_pipelets ;;
  *) usage; exit 1 ;;
esac
