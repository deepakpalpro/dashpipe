# dashpipe-ci_cd

Build, deploy, and test automation for dashpipe-suite.

## Contents

| Path | Purpose |
|------|---------|
| [scripts/localdev.sh](scripts/localdev.sh) | Suite orchestrator — Compose, API, UI, pipelet builds |
| [scripts/azure/](scripts/azure/) | ACR build/push, AKS apply, stepwise deploy, teardown |
| [scripts/](scripts/) | Smoke tests, E2E, catalog generators |
| [k8s/azure/](k8s/azure/) | AKS manifests (Kustomize) |
| [k8s/inventory/](k8s/inventory/) | Demo pipeline Job YAML |
| [azure/main.bicep](azure/main.bicep) | Azure IaC (ACR, AKS, Log Analytics) |
| [assemblies/](assemblies/) | Platform assembly pins (local, azure) |
| [docker-compose.platform.yml](docker-compose.platform.yml) | MySQL + RabbitMQ |

## Quick start

```bash
# From repo root
./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics --k8s
./dashpipe-ci_cd/scripts/localdev.sh status
```

## Azure deploy

```bash
./dashpipe-ci_cd/scripts/azure/build-push-acr.sh <acrName> 0.1.0
./dashpipe-ci_cd/scripts/azure/apply-aks.sh <acrName> 0.1.0
```

See [docs/operations/DEPLOYMENT.md](../docs/operations/DEPLOYMENT.md).

## Docs

- [Local development guide](../docs/getting-started/LOCAL_DEVELOPMENT.md)
- [K8s Azure README](k8s/azure/README.md)
- [Contributing — Development](../docs/contributing/DEVELOPMENT.md)
