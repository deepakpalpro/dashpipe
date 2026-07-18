#!/usr/bin/env bash
# Interactive, one-step-at-a-time Azure stack on AKS.
# After each step, verify in the Azure Portal, then press Enter to continue.
#
# Steps:
#   1  Create Resource Group
#   2  Create AKS cluster
#   3  Create ACR + attach to AKS
#   4  Build & push platform images (api, ui, petstore) + import MySQL/RabbitMQ
#   5  Deploy in-cluster MySQL
#   6  Deploy RabbitMQ (dashpipe broker)
#   7  Deploy petstore on AKS
#   8  Deploy dashpipe-api
#   9  Deploy dashpipe-ui
#  10  Build & push demo pipelets (rest-source, python-filter, webhook-destination)
#  11  Deploy petstore-inventory on AKS
#
# Usage:
#   ./scripts/azure/deploy-stack-stepwise.sh              # run all steps with pauses
#   ./scripts/azure/deploy-stack-stepwise.sh 3            # run only step 3
#   ./scripts/azure/deploy-stack-stepwise.sh 10 11        # pipelets + inventory only
#
# Override defaults:
#   RG=rg-dashpipe LOCATION=eastus PREFIX=dfdev TAG=0.1.0 \
#     ./scripts/azure/deploy-stack-stepwise.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
PLATFORM="$ROOT/dashpipe-platform"
DEMO="$ROOT/dashpipe-demo"
CICD="$ROOT/dashpipe-ci_cd"
OVERLAY="$CICD/k8s/azure"

# ── configurable ────────────────────────────────────────────────────────────
RG="${RG:-rg-dashpipe}"
LOCATION="${LOCATION:-eastus}"
PREFIX="${PREFIX:-dfdev}"
TAG="${TAG:-0.1.0}"
AKS_NAME="${AKS_NAME:-${PREFIX}-aks}"
AKS_VM_SIZE="${AKS_VM_SIZE:-Standard_D2s_v7}"
AKS_NODE_COUNT="${AKS_NODE_COUNT:-1}"
NAMESPACE="${NAMESPACE:-dashpipe}"

# ACR name: alphanumeric only, globally unique. Override ACR_NAME to reuse an existing registry.
ACR_NAME="${ACR_NAME:-}"

# ── helpers ──────────────────────────────────────────────────────────────────
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

need() {
  command -v "$1" >/dev/null 2>&1 || die "'$1' is required"
}

pause() {
  local msg="${1:-Verify in Azure Portal, then press Enter to continue (Ctrl-C to stop)…}"
  echo
  echo "────────────────────────────────────────────────────────────────"
  printf '⏸  %s\n' "$msg"
  echo "────────────────────────────────────────────────────────────────"
  read -r _
}

resolve_acr_name() {
  if [[ -n "$ACR_NAME" ]]; then
    return
  fi
  # Prefer an ACR already in this RG (so re-running later steps reuses step 3).
  local existing
  existing="$(az acr list -g "$RG" --query '[0].name' -o tsv 2>/dev/null || true)"
  if [[ -n "$existing" ]]; then
    ACR_NAME="$existing"
    return
  fi
  # Keep ≤50 chars, lowercase alphanumeric (ACR naming rules).
  local suffix
  suffix="$(printf '%s' "${RG}${LOCATION}" | shasum -a 256 | cut -c1-8)"
  ACR_NAME="$(printf '%sacr%s' "$PREFIX" "$suffix" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9' | cut -c1-50)"
}

login_server() {
  az acr show -n "$ACR_NAME" --query loginServer -o tsv
}

ensure_logged_in() {
  az account show >/dev/null 2>&1 || die "Not logged in. Run: az login"
  echo "Subscription: $(az account show --query name -o tsv)"
  echo "  RG=$RG  LOCATION=$LOCATION  AKS=$AKS_NAME  TAG=$TAG"
  echo
}

secret_file() {
  if [[ -f "$OVERLAY/secret.yaml" ]]; then
    echo "$OVERLAY/secret.yaml"
  else
    echo "$OVERLAY/secret.example.yaml"
  fi
}

ensure_namespace_and_secrets() {
  local sec workdir
  sec="$(secret_file)"
  workdir="$(mktemp -d)"
  # shellcheck disable=SC2064
  trap "rm -rf '$workdir'" RETURN

  cp "$OVERLAY/namespace.yaml" "$workdir/"
  cp "$sec" "$workdir/secret.yaml"
  cat >"$workdir/kustomization.yaml" <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: ${NAMESPACE}
resources:
  - namespace.yaml
  - secret.yaml
EOF
  kubectl apply -k "$workdir"
  if [[ "$(basename "$sec")" == "secret.example.yaml" ]]; then
    echo "NOTE: using secret.example.yaml (demo passwords). Copy to secret.yaml for real secrets."
  fi
}

# build_push <dockerfile> <context> <image-repo-without-tag>
# Prefixed with ACR login server + TAG. Call from repo root after az acr login.
build_push() {
  local dockerfile="$1" context="$2" image="$3"
  local server full
  server="$(login_server)"
  full="${server}/${image}:${TAG}"
  echo "---- Building $full"
  docker build -f "$dockerfile" -t "$full" "$context"
  docker push "$full"
}

# ── steps ────────────────────────────────────────────────────────────────────

step_1_resource_group() {
  echo "==> STEP 1: Create Resource Group"
  echo "    Name:     $RG"
  echo "    Location: $LOCATION"
  echo
  echo "Portal check after: Resource groups → $RG"

  az group create --name "$RG" --location "$LOCATION" --output table

  pause "Portal: Resource groups → confirm '$RG' exists in $LOCATION."
}

step_2_aks() {
  echo "==> STEP 2: Create Azure Kubernetes Service (AKS)"
  echo "    Name:      $AKS_NAME"
  echo "    Nodes:     $AKS_NODE_COUNT × $AKS_VM_SIZE"
  echo "    (takes ~5–10 minutes)"
  echo
  echo "Portal check after: Resource groups → $RG → $AKS_NAME (Kubernetes service)"

  if az aks show -g "$RG" -n "$AKS_NAME" >/dev/null 2>&1; then
    echo "AKS '$AKS_NAME' already exists — skipping create."
  else
    az aks create \
      --resource-group "$RG" \
      --name "$AKS_NAME" \
      --location "$LOCATION" \
      --node-count "$AKS_NODE_COUNT" \
      --node-vm-size "$AKS_VM_SIZE" \
      --generate-ssh-keys \
      --enable-managed-identity \
      --output table
  fi

  echo "Fetching kubeconfig…"
  az aks get-credentials --resource-group "$RG" --name "$AKS_NAME" --overwrite-existing
  kubectl get nodes -o wide

  pause "Portal: $AKS_NAME → Overview (Succeeded) → Node pools → Kubernetes resources (optional)."
}

step_3_acr() {
  echo "==> STEP 3: Create Azure Container Registry + attach to AKS"
  echo "    Name: $ACR_NAME"
  echo
  echo "Portal check after: Resource groups → $RG → $ACR_NAME (Container registry)"

  if az acr show -n "$ACR_NAME" >/dev/null 2>&1; then
    echo "ACR '$ACR_NAME' already exists — skipping create."
  else
    az acr create \
      --resource-group "$RG" \
      --name "$ACR_NAME" \
      --sku Basic \
      --location "$LOCATION" \
      --admin-enabled false \
      --output table
  fi

  echo "Attaching ACR to AKS (AcrPull)…"
  az aks update \
    --resource-group "$RG" \
    --name "$AKS_NAME" \
    --attach-acr "$ACR_NAME" \
    --output table

  echo "Login server: $(login_server)"

  pause "Portal: ACR → Access keys / Overview → then AKS → Properties (attached registry)."
}

step_4_push_images() {
  echo "==> STEP 4: Build & push platform images to ACR (+ import MySQL/RabbitMQ)"
  need docker

  local server
  server="$(login_server)"
  echo "    Server: $server"
  echo "    Tag:    $TAG"
  echo
  echo "Portal check after: ACR → Repositories → dashpipe/api, ui, petstore, mysql, rabbitmq"

  az acr login -n "$ACR_NAME"

  cd "$PLATFORM"
  build_push dashpipe-api/Dockerfile . "dashpipe/api"
  build_push dashpipe-ui/Dockerfile . "dashpipe/ui"
  build_push "$DEMO/petstore/Dockerfile" "$DEMO/petstore" "dashpipe/petstore"
  build_push "$DEMO/petstore-inventory/Dockerfile" "$DEMO/petstore-inventory" "dashpipe/petstore-inventory"

  echo "---- Importing MySQL 8.4 into ACR"
  az acr import \
    --name "$ACR_NAME" \
    --source "docker.io/library/mysql:8.4" \
    --image "dashpipe/mysql:8.4" \
    --force

  echo "---- Importing RabbitMQ 3.13-management into ACR (broker)"
  az acr import \
    --name "$ACR_NAME" \
    --source "docker.io/library/rabbitmq:3.13-management" \
    --image "dashpipe/rabbitmq:3.13-management" \
    --force

  echo
  echo "Pushed / imported:"
  echo "  ${server}/dashpipe/api:${TAG}"
  echo "  ${server}/dashpipe/ui:${TAG}"
  echo "  ${server}/dashpipe/petstore:${TAG}"
  echo "  ${server}/dashpipe/petstore-inventory:${TAG}"
  echo "  ${server}/dashpipe/mysql:8.4"
  echo "  ${server}/dashpipe/rabbitmq:3.13-management"

  pause "Portal: ACR → Repositories — confirm api, ui, petstore, mysql, rabbitmq tags."
}

step_5_mysql() {
  echo "==> STEP 5: Deploy in-cluster MySQL (creates 'pipeline' + 'petstore' DBs)"
  echo "    Namespace: $NAMESPACE"
  echo
  echo "Portal check after: AKS → Workloads → Deployments → mysql"

  local server workdir
  server="$(login_server)"
  ensure_namespace_and_secrets

  workdir="$(mktemp -d)"
  # shellcheck disable=SC2064
  trap "rm -rf '$workdir'" RETURN

  sed "s|image: mysql:8.4|image: ${server}/dashpipe/mysql:8.4|" \
    "$OVERLAY/mysql.yaml" >"$workdir/mysql.yaml"

  kubectl apply -f "$workdir/mysql.yaml"
  echo "Waiting for MySQL rollout…"
  kubectl -n "$NAMESPACE" rollout status deploy/mysql --timeout=180s

  echo
  kubectl -n "$NAMESPACE" get deploy,svc,pods -l app=mysql

  pause "Portal / kubectl: mysql pod Running + Ready. DBs pipeline + petstore created on first start."
}

step_6_rabbitmq() {
  echo "==> STEP 6: Deploy RabbitMQ (dashpipe stage broker)"
  echo "    Namespace: $NAMESPACE"
  echo "    AMQP:      rabbitmq:5672"
  echo "    Mgmt UI:   rabbitmq:15672"
  echo
  echo "Portal check after: AKS → Workloads → rabbitmq"

  local server workdir
  server="$(login_server)"
  ensure_namespace_and_secrets

  workdir="$(mktemp -d)"
  # shellcheck disable=SC2064
  trap "rm -rf '$workdir'" RETURN

  sed "s|image: rabbitmq:3.13-management|image: ${server}/dashpipe/rabbitmq:3.13-management|" \
    "$OVERLAY/rabbitmq.yaml" >"$workdir/rabbitmq.yaml"

  kubectl apply -f "$workdir/rabbitmq.yaml"
  echo "Waiting for RabbitMQ rollout…"
  kubectl -n "$NAMESPACE" rollout status deploy/rabbitmq --timeout=180s

  echo
  kubectl -n "$NAMESPACE" get deploy,svc,pods -l app=rabbitmq

  cat <<EOF

Broker endpoints (in-cluster):
  amqp://pipeline:<password>@rabbitmq.${NAMESPACE}.svc.cluster.local:5672/
  Management UI (port-forward):
    kubectl -n ${NAMESPACE} port-forward svc/rabbitmq 15672:15672
    open http://localhost:15672   (user: pipeline / pass from secret)
EOF

  pause "Portal / kubectl: rabbitmq pod Running + Ready."
}

step_7_petstore() {
  echo "==> STEP 7: Deploy petstore on AKS"
  echo "    Namespace: $NAMESPACE"
  echo
  echo "Portal check after: AKS → Workloads → petstore"

  local server workdir
  server="$(login_server)"
  workdir="$(mktemp -d)"
  # shellcheck disable=SC2064
  trap "rm -rf '$workdir'" RETURN

  sed "s|image: dashpipe/petstore:latest|image: ${server}/dashpipe/petstore:${TAG}|" \
    "$OVERLAY/petstore.yaml" >"$workdir/petstore.yaml"

  kubectl apply -f "$workdir/petstore.yaml"
  echo "Waiting for petstore rollout…"
  kubectl -n "$NAMESPACE" rollout status deploy/petstore --timeout=180s

  echo
  kubectl -n "$NAMESPACE" get deploy,svc,pods -l app=petstore

  cat <<EOF

Petstore is up in-cluster:
  http://petstore.${NAMESPACE}.svc.cluster.local:4010

Health check (local port-forward):
  kubectl -n ${NAMESPACE} port-forward svc/petstore 4010:4010
  curl -s http://localhost:4010/health
EOF

  pause "Portal: petstore deployment Ready."
}

step_8_api() {
  echo "==> STEP 8: Deploy dashpipe-api"
  echo "    Namespace: $NAMESPACE"
  echo "    Depends on: MySQL (step 5) + RabbitMQ (step 6)"
  echo
  echo "Portal check after: AKS → Workloads → dashpipe-api"

  local server workdir image_pattern
  server="$(login_server)"
  image_pattern="${server}/dashpipe/{pipeletId}:${TAG}"
  ensure_namespace_and_secrets

  workdir="$(mktemp -d)"
  # shellcheck disable=SC2064
  trap "rm -rf '$workdir'" RETURN

  cp "$OVERLAY/rbac.yaml" "$workdir/rbac.yaml"
  sed \
    -e "s|image: dashpipe/api:latest|image: ${server}/dashpipe/api:${TAG}|" \
    -e "s|PIPELINE_K8S_DEFAULT_IMAGE_PATTERN: \"dashpipe/{pipeletId}:latest\"|PIPELINE_K8S_DEFAULT_IMAGE_PATTERN: \"${image_pattern}\"|" \
    "$OVERLAY/api.yaml" >"$workdir/api.yaml"

  kubectl apply -f "$workdir/rbac.yaml"
  kubectl apply -f "$workdir/api.yaml"
  echo "Waiting for dashpipe-api rollout…"
  kubectl -n "$NAMESPACE" rollout status deploy/dashpipe-api --timeout=300s

  echo
  kubectl -n "$NAMESPACE" get deploy,svc,pods -l app=dashpipe-api

  cat <<EOF

API health (port-forward):
  kubectl -n ${NAMESPACE} port-forward svc/dashpipe-api 8080:8080
  curl -s http://localhost:8080/actuator/health

Pipelet Jobs will pull: ${image_pattern}
EOF

  pause "Portal / kubectl: dashpipe-api Ready; health endpoint returns UP."
}

step_9_ui() {
  echo "==> STEP 9: Deploy dashpipe-ui (LoadBalancer)"
  echo "    Namespace: $NAMESPACE"
  echo "    Depends on: dashpipe-api (step 8) for /api proxy"
  echo
  echo "Portal check after: AKS → Workloads → dashpipe-ui → Services (EXTERNAL-IP)"

  local server workdir
  server="$(login_server)"
  workdir="$(mktemp -d)"
  # shellcheck disable=SC2064
  trap "rm -rf '$workdir'" RETURN

  sed "s|image: dashpipe/ui:latest|image: ${server}/dashpipe/ui:${TAG}|" \
    "$OVERLAY/ui.yaml" >"$workdir/ui.yaml"

  kubectl apply -f "$workdir/ui.yaml"
  echo "Waiting for dashpipe-ui rollout…"
  kubectl -n "$NAMESPACE" rollout status deploy/dashpipe-ui --timeout=180s

  echo
  kubectl -n "$NAMESPACE" get deploy,svc,pods -l app=dashpipe-ui

  cat <<EOF

UI service is type LoadBalancer — EXTERNAL-IP may take 1–2 minutes:
  kubectl -n ${NAMESPACE} get svc dashpipe-ui -w

Then open http://<EXTERNAL-IP>/

Local fallback:
  kubectl -n ${NAMESPACE} port-forward svc/dashpipe-ui 8088:80
  open http://localhost:8088/
EOF

  pause "Portal: dashpipe-ui Ready + LoadBalancer EXTERNAL-IP assigned."
}

step_10_push_pipelets() {
  echo "==> STEP 10: Build & push demo pipelet images to ACR"
  echo "    Pipelets: plet-rest-source, plet-python-filter, plet-webhook-destination"
  echo "    Tag:      $TAG"
  echo
  echo "Portal check after: ACR → Repositories → dashpipe/plet-*"
  need docker

  local server
  server="$(login_server)"
  az acr login -n "$ACR_NAME"

  cd "$PLATFORM"
  # Context is pipelets/ (Dockerfiles COPY _common + relative paths from there).
  build_push \
    pipelets/source/http/plet-rest-source/Dockerfile \
    pipelets \
    "dashpipe/plet-rest-source"
  build_push \
    pipelets/transformer/filter/plet-python-filter/Dockerfile \
    pipelets \
    "dashpipe/plet-python-filter"
  build_push \
    pipelets/destination/notify/plet-webhook-destination/Dockerfile \
    pipelets \
    "dashpipe/plet-webhook-destination"

  echo
  echo "Pushed:"
  echo "  ${server}/dashpipe/plet-rest-source:${TAG}"
  echo "  ${server}/dashpipe/plet-python-filter:${TAG}"
  echo "  ${server}/dashpipe/plet-webhook-destination:${TAG}"
  echo
  echo "API Job pattern (step 8): ${server}/dashpipe/{pipeletId}:${TAG}"

  pause "Portal: ACR → Repositories — confirm the three plet-* images."
}

step_11_petstore_inventory() {
  echo "==> STEP 11: Deploy petstore-inventory on AKS"
  echo "    Namespace: $NAMESPACE"
  echo "    Image pushed in step 4 (dashpipe/petstore-inventory)"
  echo
  echo "Portal check after: AKS → Workloads → petstore-inventory"

  local server workdir
  server="$(login_server)"
  workdir="$(mktemp -d)"
  # shellcheck disable=SC2064
  trap "rm -rf '$workdir'" RETURN

  sed "s|image: dashpipe/petstore-inventory:latest|image: ${server}/dashpipe/petstore-inventory:${TAG}|" \
    "$OVERLAY/petstore-inventory.yaml" >"$workdir/petstore-inventory.yaml"

  kubectl apply -f "$workdir/petstore-inventory.yaml"
  echo "Waiting for petstore-inventory rollout…"
  kubectl -n "$NAMESPACE" rollout status deploy/petstore-inventory --timeout=120s

  echo
  kubectl -n "$NAMESPACE" get deploy,svc,pods -l app=petstore-inventory

  cat <<EOF

petstore-inventory is up in-cluster (REST source for plet-rest-source demos):
  http://petstore-inventory.${NAMESPACE}.svc.cluster.local:4011

Health check (local port-forward):
  kubectl -n ${NAMESPACE} port-forward svc/petstore-inventory 4011:4011
  curl -s http://localhost:4011/health
EOF

  pause "Portal: petstore-inventory Ready. Stack complete."
}

# ── main ─────────────────────────────────────────────────────────────────────

need az
need kubectl
ensure_logged_in
resolve_acr_name
echo "Resolved ACR_NAME=$ACR_NAME"
echo

usage() {
  cat <<EOF
Usage: $(basename "$0") [step_numbers…]

  (no args)   run steps 1→11 with a pause after each
  3           run only step 3
  10 11       pipelets + petstore-inventory only

Steps:
  1  Create Resource Group
  2  Create AKS
  3  Create ACR + attach to AKS
  4  Push platform images (api, ui, petstore) + import MySQL/RabbitMQ
  5  Deploy MySQL on AKS
  6  Deploy RabbitMQ (dashpipe broker)
  7  Deploy petstore on AKS
  8  Deploy dashpipe-api
  9  Deploy dashpipe-ui
 10  Push demo pipelets (plet-rest-source, plet-python-filter, plet-webhook-destination)
 11  Deploy petstore-inventory on AKS
EOF
}

run_step() {
  case "$1" in
    1) step_1_resource_group ;;
    2) step_2_aks ;;
    3) step_3_acr ;;
    4) step_4_push_images ;;
    5) step_5_mysql ;;
    6) step_6_rabbitmq ;;
    7) step_7_petstore ;;
    8) step_8_api ;;
    9) step_9_ui ;;
    10) step_10_push_pipelets ;;
    11) step_11_petstore_inventory ;;
    -h|--help|help) usage; exit 0 ;;
    *) die "Unknown step '$1' (valid: 1–11). Try --help" ;;
  esac
}

if [[ $# -eq 0 ]]; then
  STEPS=(1 2 3 4 5 6 7 8 9 10 11)
else
  STEPS=("$@")
fi

echo "Will run steps: ${STEPS[*]}"
echo "Press Enter to start (Ctrl-C to abort)…"
read -r _

for s in "${STEPS[@]}"; do
  run_step "$s"
done

echo
echo "All requested steps finished."
