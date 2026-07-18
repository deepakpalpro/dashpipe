# Azure AKS Wave A — empty control plane

Self-contained Dashpipe on AKS with **in-cluster MySQL + RabbitMQ**, platform images from **ACR**, petstore mocks, Prometheus, and Grafana.

**Empty product defaults:** no Flyway demo seed (no pipelines / demo connectors), **all catalog pipelets inactive**, **no pipelet images** pushed unless you pass `--all`.

Wave B (Azure MySQL Flexible Server + Service Bus) stays documented in [`docs/AZURE_ASSEMBLY.md`](../../../docs/AZURE_ASSEMBLY.md).

## What this deploys

| Workload | Service | Notes |
|----------|---------|-------|
| `dashpipe-api` | `dashpipe-api:8080` | Profile `k8s`; schema-only Flyway |
| `dashpipe-ui` | `dashpipe-ui:80` (LoadBalancer) | nginx proxies `/api` → API; inactive pipelets |
| `mysql` | `mysql:3306` | DBs `pipeline` + `petstore` |
| `rabbitmq` | `rabbitmq:5672` | AMQP for stage queues (when you activate pipelets) |
| `petstore` | `petstore:4010` | Optional webhook / upload sink |
| `petstore-inventory` | `petstore-inventory:4011` | Optional REST source demo |
| `prometheus` | `prometheus:9090` | Scrapes API `/actuator/prometheus` |
| `grafana` | `grafana:3000` | Admin password from secret |

## Prerequisites

```bash
az login
az group create -n rg-dashpipe -l eastus

# Infra (ACR + AKS + …) — or reuse an existing AKS+ACR
az deployment group create \
  --resource-group rg-dashpipe \
  --template-file deploy/azure/main.bicep \
  --parameters namePrefix=dfdev aksVmSize=Standard_D2s_v7 aksNodeCount=1 deployManagedDataPlane=false

AKS=$(az deployment group show -g rg-dashpipe -n main --query properties.outputs.aksName.value -o tsv)
ACR=$(az deployment group show -g rg-dashpipe -n main --query properties.outputs.acrName.value -o tsv)

az aks get-credentials -g rg-dashpipe -n "$AKS"
az aks update -g rg-dashpipe -n "$AKS" --attach-acr "$ACR"
```

Docker Desktop / Rancher must be able to build and push.

## Build & push images (platform only)

```bash
chmod +x scripts/azure/*.sh
./scripts/azure/build-push-acr.sh "$ACR" 0.1.0 --platform-only
```

Pushes / imports:

- `$LOGIN_SERVER/dashpipe/api:0.1.0`
- `$LOGIN_SERVER/dashpipe/ui:0.1.0`
- `$LOGIN_SERVER/dashpipe/petstore:0.1.0`
- `$LOGIN_SERVER/dashpipe/petstore-inventory:0.1.0`
- `$LOGIN_SERVER/dashpipe/mysql:8.4`
- `$LOGIN_SERVER/dashpipe/rabbitmq:3.13-management`
- `$LOGIN_SERVER/dashpipe/prometheus:v2.55.1`
- `$LOGIN_SERVER/dashpipe/grafana:11.3.1`

Omit `--platform-only` / pass `--all` only when you intentionally build every pipelet image.

## Apply to AKS

```bash
./scripts/azure/apply-aks.sh "$ACR" 0.1.0
kubectl -n dashpipe get pods,svc
kubectl -n dashpipe get svc dashpipe-ui -w   # wait for EXTERNAL-IP
```

Optional: copy `secret.example.yaml` → `secret.yaml` and change passwords before apply (keep AMQP URL in `api.yaml` ConfigMap in sync with RabbitMQ password, or leave demo defaults). Include `grafana-admin-password`.

## Empty stack expectations

| Surface | Expected |
|---------|----------|
| Pipelines | None |
| Connectors / tenants seed | None (V19–V21 live under `db/seed`, only with Spring profile `local`) |
| Pipelet catalog | Visible but all `active: false`; builder palette empty |
| Pipelet Jobs | Will fail until you activate a pipelet and push its image |

Local Compose still uses profile `local` and applies `classpath:db/seed` for demo connectors.

## Demo connector URLs (in-cluster, when you configure connectors later)

```text
http://petstore-inventory.dashpipe.svc.cluster.local:4011
http://petstore.dashpipe.svc.cluster.local:4010
```

## Verify

```bash
kubectl -n dashpipe logs deploy/dashpipe-api --tail=50
kubectl -n dashpipe port-forward svc/dashpipe-api 8080:8080
curl -s http://localhost:8080/actuator/health
kubectl -n dashpipe port-forward svc/grafana 3000:3000
```

## Tear down

In-cluster only:

```bash
kubectl delete namespace dashpipe
# optional: kubectl delete clusterrole,clusterrolebinding dashpipe-api-pipelet-jobs
```

Full Azure stack (resource group + AKS node RG):

```bash
./scripts/azure/teardown-stack.sh            # confirm by typing rg-dashpipe
./scripts/azure/teardown-stack.sh --yes      # no prompt
./scripts/azure/teardown-stack.sh --k8s-only # namespaces only
./scripts/azure/teardown-stack.sh --list     # preview resources
```
