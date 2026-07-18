#!/usr/bin/env bash
# Build platform images and optionally mirror deps into Azure Container Registry.
#
# Usage:
#   ./scripts/azure/build-push-acr.sh <acrName> [tag] [--platform-only|--all]
#
#   --platform-only  (default) api, ui, petstore mocks + mirror MySQL/Rabbit/Grafana/Prometheus
#   --all            also build every pipelet image (slow)
#
# Prerequisites: az login, docker, az acr login permission.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
PLATFORM="$ROOT/dashflow-platform"
DEMO="$ROOT/dashflow-demo"
CICD="$ROOT/dashflow-ci_cd"
ACR_NAME="${1:-}"
TAG="${2:-0.1.0}"
MODE="${3:---platform-only}"

if [[ -z "$ACR_NAME" ]]; then
  printf 'Usage: %s <acrName> [tag] [--platform-only|--all]\n' "$(basename "$0")" >&2
  exit 1
fi

if [[ "$TAG" == --* ]]; then
  MODE="$TAG"
  TAG="0.1.0"
fi

if [[ "$MODE" != "--platform-only" && "$MODE" != "--all" ]]; then
  printf 'Unknown mode %s (use --platform-only or --all)\n' "$MODE" >&2
  exit 1
fi

if ! command -v az >/dev/null 2>&1; then
  echo "ERROR: az CLI required" >&2
  exit 1
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker required" >&2
  exit 1
fi

LOGIN_SERVER="$(az acr show -n "$ACR_NAME" --query loginServer -o tsv)"
echo "ACR login server: $LOGIN_SERVER"
az acr login -n "$ACR_NAME"

build_push() {
  local dockerfile="$1"
  local context="$2"
  local image="$3"
  local full="${LOGIN_SERVER}/${image}:${TAG}"
  echo "==> Building $full"
  docker build -f "$dockerfile" -t "$full" "$context"
  docker push "$full"
  docker tag "$full" "${image}:${TAG}"
}

# Mirror a public image into ACR (no local docker pull required when import works).
acr_import() {
  local source="$1"
  local target="$2"
  echo "==> Importing $source → ${LOGIN_SERVER}/${target}"
  az acr import \
    --name "$ACR_NAME" \
    --source "$source" \
    --image "$target" \
    --force
}

cd "$PLATFORM"

# Platform
build_push dashflow-api/Dockerfile . "dashflow/api"
build_push dashflow-ui/Dockerfile . "dashflow/ui"

# Mocks
build_push "$DEMO/petstore/Dockerfile" "$DEMO/petstore" "dashflow/petstore"
build_push "$DEMO/petstore-inventory/Dockerfile" "$DEMO/petstore-inventory" "dashflow/petstore-inventory"

# Runtime deps (Wave A empty stack)
acr_import "docker.io/library/mysql:8.4" "dashflow/mysql:8.4"
acr_import "docker.io/library/rabbitmq:3.13-management" "dashflow/rabbitmq:3.13-management"
acr_import "docker.io/prom/prometheus:v2.55.1" "dashflow/prometheus:v2.55.1"
acr_import "docker.io/grafana/grafana:11.3.1" "dashflow/grafana:11.3.1"

if [[ "$MODE" == "--all" ]]; then
  echo "==> Building all pipelet images (--all)"
  while IFS= read -r _df; do
    _p="$(basename "$(dirname "$_df")")"
    build_push "$_df" "$PLATFORM/pipelets" "dashflow/${_p}"
  done < <(
    find "$PLATFORM/pipelets/source" "$PLATFORM/pipelets/transformer" "$PLATFORM/pipelets/destination" \
      -mindepth 2 -maxdepth 2 -type d -name 'plet-*' -exec test -f '{}/Dockerfile' \; \
      -print 2>/dev/null | sort | while read -r _d; do printf '%s/Dockerfile\n' "$_d"; done
  )
  if [[ -f "$PLATFORM/pipelets/inventory/Dockerfile" ]]; then
    build_push "$PLATFORM/pipelets/inventory/Dockerfile" "$PLATFORM/pipelets/inventory" "dashflow/inventory-pipelet"
  fi
else
  echo "==> Skipping pipelet images (--platform-only). Catalog is inactive until you activate + push."
fi

cat <<EOF

Pushed images to ${LOGIN_SERVER} (tag ${TAG} for custom images).

Custom:
  ${LOGIN_SERVER}/dashflow/api:${TAG}
  ${LOGIN_SERVER}/dashflow/ui:${TAG}
  ${LOGIN_SERVER}/dashflow/petstore:${TAG}
  ${LOGIN_SERVER}/dashflow/petstore-inventory:${TAG}

Mirrored:
  ${LOGIN_SERVER}/dashflow/mysql:8.4
  ${LOGIN_SERVER}/dashflow/rabbitmq:3.13-management
  ${LOGIN_SERVER}/dashflow/prometheus:v2.55.1
  ${LOGIN_SERVER}/dashflow/grafana:11.3.1

Next:
  ./scripts/azure/apply-aks.sh ${ACR_NAME} ${TAG}
EOF
