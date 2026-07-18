#!/usr/bin/env bash
# Tear down Dashpipe Azure resources created by deploy-stack-stepwise.sh / Bicep.
#
# Default: delete the whole resource group (AKS, ACR, disks, LoadBalancers, …).
# Also removes orphaned AKS node resource group (MC_*) when present.
#
# Usage:
#   ./scripts/azure/teardown-stack.sh                  # interactive confirm
#   ./scripts/azure/teardown-stack.sh --yes            # no prompt
#   ./scripts/azure/teardown-stack.sh --k8s-only       # only K8s workloads / namespaces
#   ./scripts/azure/teardown-stack.sh --list           # show what would be deleted
#
# Override:
#   RG=rg-dashpipe AKS_NAME=dfdev-aks PREFIX=dfdev \
#     ./scripts/azure/teardown-stack.sh

set -euo pipefail

RG="${RG:-rg-dashpipe}"
PREFIX="${PREFIX:-dfdev}"
AKS_NAME="${AKS_NAME:-${PREFIX}-aks}"
NAMESPACE="${NAMESPACE:-dashpipe}"
LOCATION="${LOCATION:-eastus}"

YES=false
K8S_ONLY=false
LIST_ONLY=false

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

need() {
  command -v "$1" >/dev/null 2>&1 || die "'$1' is required"
}

usage() {
  cat <<EOF
Usage: $(basename "$0") [--yes] [--k8s-only] [--list] [--help]

  (default)   Delete resource group '$RG' (+ orphaned AKS node RG if found)
  --k8s-only  Only remove Dashpipe/tenant namespaces + cluster RBAC (keep AKS/ACR)
  --yes       Skip confirmation prompts
  --list      Print resources in RG and exit

Env: RG  AKS_NAME  PREFIX  NAMESPACE
EOF
}

for arg in "$@"; do
  case "$arg" in
    --yes|-y) YES=true ;;
    --k8s-only) K8S_ONLY=true ;;
    --list) LIST_ONLY=true ;;
    -h|--help|help) usage; exit 0 ;;
    *) die "Unknown argument: $arg (try --help)" ;;
  esac
done

need az
az account show >/dev/null 2>&1 || die "Not logged in. Run: az login"

echo "Subscription: $(az account show --query name -o tsv)"
echo "  RG=$RG  AKS=$AKS_NAME  NAMESPACE=$NAMESPACE"
echo

confirm() {
  local prompt="$1"
  if [[ "$YES" == true ]]; then
    return 0
  fi
  printf '%s\n' "$prompt"
  printf 'Type %s to confirm: ' "$RG"
  read -r answer
  [[ "$answer" == "$RG" ]] || die "Aborted (expected '$RG')."
}

list_rg() {
  if ! az group show -n "$RG" >/dev/null 2>&1; then
    echo "Resource group '$RG' does not exist."
    return 0
  fi
  echo "==> Resources in $RG:"
  az resource list -g "$RG" -o table
  echo
  local node_rg
  node_rg="$(az aks show -g "$RG" -n "$AKS_NAME" --query nodeResourceGroup -o tsv 2>/dev/null || true)"
  if [[ -n "$node_rg" ]]; then
    echo "==> AKS node resource group: $node_rg"
    az resource list -g "$node_rg" -o table 2>/dev/null || true
  fi
}

cleanup_k8s() {
  if ! command -v kubectl >/dev/null 2>&1; then
    echo "kubectl not found — skipping in-cluster cleanup."
    return 0
  fi
  if ! kubectl cluster-info >/dev/null 2>&1; then
    echo "No working kube context — skipping in-cluster cleanup."
    echo "  (Optional: az aks get-credentials -g $RG -n $AKS_NAME)"
    return 0
  fi

  echo "==> Deleting Dashpipe workloads / tenant namespaces…"
  kubectl delete namespace "$NAMESPACE" --ignore-not-found --wait=false
  # Tenant Jobs namespaces created by the API (tenant-t001, …)
  local ns
  while IFS= read -r ns; do
    [[ -z "$ns" ]] && continue
    echo "    delete namespace $ns"
    kubectl delete namespace "$ns" --ignore-not-found --wait=false
  done < <(kubectl get ns -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null | grep -E '^tenant-' || true)

  echo "==> Deleting cluster RBAC for pipelet Jobs…"
  kubectl delete clusterrolebinding dashpipe-api-pipelet-jobs --ignore-not-found
  kubectl delete clusterrole dashpipe-api-pipelet-jobs --ignore-not-found

  echo "K8s cleanup requested (namespaces may take a minute to terminate)."
}

delete_resource_group() {
  local name="$1"
  if ! az group show -n "$name" >/dev/null 2>&1; then
    echo "Resource group '$name' already gone — skip."
    return 0
  fi
  echo "==> Deleting resource group $name (this can take several minutes)…"
  az group delete -n "$name" --yes --no-wait
  echo "    Delete started for $name (--no-wait). Monitor in Portal → Resource groups."
}

# ── main ─────────────────────────────────────────────────────────────────────

if [[ "$LIST_ONLY" == true ]]; then
  list_rg
  exit 0
fi

if [[ "$K8S_ONLY" == true ]]; then
  confirm "This will DELETE Kubernetes namespaces '$NAMESPACE' + tenant-* (AKS/ACR kept)."
  cleanup_k8s
  echo
  echo "Done (k8s-only). Azure RG '$RG' was NOT deleted."
  exit 0
fi

list_rg
echo

# Capture node RG before AKS disappears
NODE_RG=""
if az aks show -g "$RG" -n "$AKS_NAME" >/dev/null 2>&1; then
  NODE_RG="$(az aks show -g "$RG" -n "$AKS_NAME" --query nodeResourceGroup -o tsv 2>/dev/null || true)"
fi

confirm "This will DELETE resource group '$RG' and ALL resources inside (AKS, ACR, …).${NODE_RG:+ Also node RG '$NODE_RG'.}"

# Best-effort kube cleanup first (faster LB / PVC release)
cleanup_k8s || true

delete_resource_group "$RG"

# AKS node RG (MC_*) is often separate; delete if still present / known
if [[ -n "$NODE_RG" ]]; then
  # Small wait so control-plane unlink can start
  sleep 5
  delete_resource_group "$NODE_RG"
else
  # Heuristic: orphaned MC_<rg>_* in same subscription
  while IFS= read -r orphan; do
    [[ -z "$orphan" ]] && continue
    echo "==> Found possible AKS node RG: $orphan"
    if [[ "$YES" == true ]]; then
      delete_resource_group "$orphan"
    else
      printf 'Delete %s as well? [y/N] ' "$orphan"
      read -r ans
      if [[ "$ans" == [yY] ]]; then
        delete_resource_group "$orphan"
      fi
    fi
  done < <(
    az group list --query "[?starts_with(name, 'MC_${RG}_')].name" -o tsv 2>/dev/null || true
  )
fi

cat <<EOF

Teardown started.
  Portal: Resource groups → confirm '$RG'${NODE_RG:+ and '$NODE_RG'} disappear (or show Deleting).

Watch status:
  az group list --query "[?name=='$RG' || starts_with(name, 'MC_${RG}_')].{name:name,state:properties.provisioningState}" -o table

EOF
