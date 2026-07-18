#!/usr/bin/env bash
# Apply empty Wave A Dashflow stack to the current kubectl context (AKS).
# No pipelet images required; Flyway schema only (no demo pipelines/connectors).
#
# Usage:
#   ./scripts/azure/apply-aks.sh <acrName> [tag]
# Example:
#   ./scripts/azure/apply-aks.sh dfdevacrxxxx 0.1.0
#
# Prerequisites:
#   - az aks get-credentials ...
#   - az aks update --attach-acr <acrName>
#   - images already pushed (build-push-acr.sh --platform-only)

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
PLATFORM="$ROOT/dashflow-platform"
DEMO="$ROOT/dashflow-demo"
CICD="$ROOT/dashflow-ci_cd"
OVERLAY="$CICD/k8s/azure"

ACR_NAME="${1:-}"
TAG="${2:-0.1.0}"

if [[ -z "$ACR_NAME" ]]; then
  printf 'Usage: %s <acrName> [tag]\n' "$(basename "$0")" >&2
  exit 1
fi

LOGIN_SERVER="$(az acr show -n "$ACR_NAME" --query loginServer -o tsv)"
IMAGE_PATTERN="${LOGIN_SERVER}/dashflow/{pipeletId}:${TAG}"

echo "Using ACR: $LOGIN_SERVER  tag: $TAG"
echo "Pipelet image pattern (unused until pipelets activated): $IMAGE_PATTERN"

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT
cp -R "$OVERLAY"/. "$WORKDIR"/

SECRET_FILE="secret.example.yaml"
if [[ -f "$OVERLAY/secret.yaml" ]]; then
  SECRET_FILE="secret.yaml"
  cp "$OVERLAY/secret.yaml" "$WORKDIR/secret.yaml"
  echo "Using deploy/k8s/azure/secret.yaml"
else
  echo "NOTE: using secret.example.yaml (demo passwords). Copy to secret.yaml for real secrets."
fi

# Rewrite kustomization with ACR image refs + chosen secret
cat >"$WORKDIR/kustomization.yaml" <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: dashflow
resources:
  - namespace.yaml
  - rbac.yaml
  - ${SECRET_FILE}
  - mysql.yaml
  - rabbitmq.yaml
  - api.yaml
  - ui.yaml
  - petstore.yaml
  - petstore-inventory.yaml
  - prometheus.yaml
  - grafana.yaml
images:
  - name: dashflow/api
    newName: ${LOGIN_SERVER}/dashflow/api
    newTag: "${TAG}"
  - name: dashflow/ui
    newName: ${LOGIN_SERVER}/dashflow/ui
    newTag: "${TAG}"
  - name: dashflow/petstore
    newName: ${LOGIN_SERVER}/dashflow/petstore
    newTag: "${TAG}"
  - name: dashflow/petstore-inventory
    newName: ${LOGIN_SERVER}/dashflow/petstore-inventory
    newTag: "${TAG}"
  - name: mysql
    newName: ${LOGIN_SERVER}/dashflow/mysql
    newTag: "8.4"
  - name: rabbitmq
    newName: ${LOGIN_SERVER}/dashflow/rabbitmq
    newTag: "3.13-management"
  - name: prom/prometheus
    newName: ${LOGIN_SERVER}/dashflow/prometheus
    newTag: "v2.55.1"
  - name: grafana/grafana
    newName: ${LOGIN_SERVER}/dashflow/grafana
    newTag: "11.3.1"
EOF

python3 - <<PY
from pathlib import Path
p = Path("${WORKDIR}/api.yaml")
text = p.read_text()
old = 'PIPELINE_K8S_DEFAULT_IMAGE_PATTERN: "dashflow/{pipeletId}:latest"'
new = 'PIPELINE_K8S_DEFAULT_IMAGE_PATTERN: "${IMAGE_PATTERN}"'
if old not in text:
    raise SystemExit("Could not find PIPELINE_K8S_DEFAULT_IMAGE_PATTERN in api.yaml")
p.write_text(text.replace(old, new, 1))
print("Patched API image pattern →", new.split(": ", 1)[-1])
PY

echo "Applying manifests…"
kubectl apply -k "$WORKDIR"

echo "Waiting for core workloads…"
kubectl -n dashflow rollout status deploy/mysql --timeout=180s || true
kubectl -n dashflow rollout status deploy/rabbitmq --timeout=180s || true
kubectl -n dashflow rollout status deploy/dashflow-api --timeout=300s || true
kubectl -n dashflow rollout status deploy/dashflow-ui --timeout=180s || true
kubectl -n dashflow rollout status deploy/petstore --timeout=180s || true
kubectl -n dashflow rollout status deploy/petstore-inventory --timeout=120s || true
kubectl -n dashflow rollout status deploy/prometheus --timeout=120s || true
kubectl -n dashflow rollout status deploy/grafana --timeout=120s || true

cat <<EOF

Empty Wave A applied in namespace 'dashflow'.
- No demo pipelines / connector seed (Flyway schema only)
- All catalog pipelets are inactive in the UI

UI (LoadBalancer — may take a minute):
  kubectl -n dashflow get svc dashflow-ui -w

Grafana / Prometheus (cluster-internal):
  kubectl -n dashflow port-forward svc/grafana 3000:3000
  kubectl -n dashflow port-forward svc/prometheus 9090:9090

API health:
  kubectl -n dashflow port-forward svc/dashflow-api 8080:8080
  curl -s http://localhost:8080/actuator/health
EOF
