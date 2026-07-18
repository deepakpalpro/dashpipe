# Azure assembly

First cloud assembly for Dashflow.

## Stack

| Concern | Azure service |
|---------|---------------|
| Runtime | AKS (Kubernetes Jobs via Fabric8) |
| Metadata DB | Azure Database for MySQL Flexible Server *(Wave B)* / in-cluster MySQL *(Wave A)* |
| Platform stage bus | Azure Service Bus *(Wave B)* / in-cluster RabbitMQ *(Wave A)* |
| Images | Azure Container Registry |
| Observability | Log Analytics + Container Insights |

**Not in this assembly (Phase 2):** Azure Blob, Event Hubs, connector/pipelet cloud providers.

## Wave A — empty control plane on AKS (recommended first)

In-cluster MySQL + RabbitMQ, ACR platform images, UI/API/mocks/metrics on AKS.  
**No pipelet images by default; no demo pipelines; all catalog pipelets inactive.**

Full steps: [`deploy/k8s/azure/README.md`](../deploy/k8s/azure/README.md)

```bash
# After Bicep + attach-acr + kubectl credentials:
./scripts/azure/build-push-acr.sh <acrName> 0.1.0 --platform-only
./scripts/azure/apply-aks.sh <acrName> 0.1.0
```

## Wave B — activate Azure MySQL + Service Bus

```bash
export DASHFLOW_MYSQL_URL='jdbc:mysql://<server>.mysql.database.azure.com:3306/dashflow?useSSL=true'
export DASHFLOW_MYSQL_USER='dashflow'
export DASHFLOW_MYSQL_PASSWORD='…'
export DASHFLOW_SERVICEBUS_CONNECTION_STRING='Endpoint=sb://…'
export DASHFLOW_SERVICEBUS_NAMESPACE='….servicebus.windows.net'
export DASHFLOW_IMAGE_TAG='0.1.0'
# Point Jobs at ACR, e.g.:
export PIPELINE_K8S_DEFAULT_IMAGE_PATTERN='<acr>.azurecr.io/dashflow/{pipeletId}:0.1.0'

java -jar dashflow-api.jar --spring.profiles.active=azure
```

Profile sets `dashflow.platform.broker=servicebus` and excludes RabbitMQ auto-configuration.

## IaC

Bicep entrypoint: [`deploy/azure/main.bicep`](../deploy/azure/main.bicep)

```bash
az deployment group create \
  --resource-group rg-dashflow \
  --template-file deploy/azure/main.bicep \
  --parameters namePrefix=dfdev aksVmSize=Standard_D2s_v7 aksNodeCount=1 deployManagedDataPlane=false
```

Wave A defaults: **AKS + ACR + Log Analytics only** (in-cluster MySQL/RabbitMQ). Set `deployManagedDataPlane=true` and `mysqlAdminPassword=…` only for Wave B.

## Local vs Azure

| | Local | Wave A (AKS) | Wave B (azure profile) |
|-|-------|--------------|------------------------|
| Profile | `local` / `local,k8s` | `local,k8s` | `azure` |
| Broker | RabbitMQ (Compose) | RabbitMQ (in-cluster) | Service Bus |
| Pipelet env | `IO_BROKER=rabbitmq`, `AMQP_URL` | same (cluster DNS) | `IO_BROKER=servicebus`, `SERVICEBUS_*` |
| Images | `dashflow/plet-*:local` | `$ACR/dashflow/plet-*:tag` | same ACR pattern |

Pipelets use [`pipelets/_common/io_transport.py`](../pipelets/_common/io_transport.py) and select the transport via `IO_BROKER`.
