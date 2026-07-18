# Deployment

How to run Dashpipe locally and on Azure AKS.

## Local (Docker Compose)

Root [docker-compose.yml](../../docker-compose.yml) orchestrates three included stacks.

### Profiles

| Profile | Services | Command |
|---------|----------|---------|
| (default) | mysql, rabbitmq, localstack | `docker compose up -d` |
| `petstore` | + petstore, petstore-inventory | `docker compose --profile petstore up -d` |
| `metrics` | + prometheus, grafana | `docker compose --profile metrics up -d` |
| `elk` | + elasticsearch, kibana, logstash | `docker compose --profile elk up -d` |

Recommended for development:

```bash
./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics
```

Compose files:

- [docker-compose.platform.yml](../../dashpipe-ci_cd/docker-compose.platform.yml)
- [docker-compose.demo.yml](../../dashpipe-demo/docker-compose.demo.yml)
- [docker-compose.tools.yml](../../dashpipe-tools/docker-compose.tools.yml)

### Assembly pin

Local assembly definition: [assemblies/local.yaml](../../dashpipe-ci_cd/assemblies/local.yaml)

---

## Azure AKS (Wave A)

Deploy platform images to AKS with in-cluster MySQL and RabbitMQ.

### Prerequisites

- Azure subscription, `az` CLI, `kubectl`
- ACR for container images
- Images pushed via build script

### Steps

```bash
# 1. Build and push platform images
./dashpipe-ci_cd/scripts/azure/build-push-acr.sh <acrName> 0.1.0

# 2. Apply K8s manifests to AKS
./dashpipe-ci_cd/scripts/azure/apply-aks.sh <acrName> 0.1.0
```

Interactive stepwise deploy: [deploy-stack-stepwise.sh](../../dashpipe-ci_cd/scripts/azure/deploy-stack-stepwise.sh)

### Documentation

| Doc | Content |
|-----|---------|
| [AZURE_ASSEMBLY.md](../../dashpipe-platform/docs/AZURE_ASSEMBLY.md) | Architecture, Bicep, Wave A vs B |
| [k8s/azure/README.md](../../dashpipe-ci_cd/k8s/azure/README.md) | Manifest layout, secrets, kustomize |
| [azure/main.bicep](../../dashpipe-ci_cd/azure/main.bicep) | IaC for ACR, AKS, Log Analytics |

### Azure assembly pin

[assemblies/azure.yaml](../../dashpipe-ci_cd/assemblies/azure.yaml)

---

## Production considerations (planned)

Wave 7 covers HA, backup, and runbooks. Current release targets **development and demo** deployments. Review [SECURITY.md](../../SECURITY.md) before exposing any instance to the internet.
