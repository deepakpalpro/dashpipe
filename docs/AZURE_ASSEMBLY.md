# Azure assembly

First cloud assembly for Dashflow.

## Stack

| Concern | Azure service |
|---------|---------------|
| Runtime | AKS (Kubernetes Jobs via Fabric8) |
| Metadata DB | Azure Database for MySQL Flexible Server |
| Platform stage bus | Azure Service Bus (queues + DLQ) |
| Images | Azure Container Registry |
| Observability | Log Analytics + Container Insights |

**Not in this assembly (Phase 2):** Azure Blob, Event Hubs, connector/pipelet cloud providers.

## Activate

```bash
export DASHFLOW_MYSQL_URL='jdbc:mysql://<server>.mysql.database.azure.com:3306/dashflow?useSSL=true'
export DASHFLOW_MYSQL_USER='dashflow'
export DASHFLOW_MYSQL_PASSWORD='…'
export DASHFLOW_SERVICEBUS_CONNECTION_STRING='Endpoint=sb://…'
export DASHFLOW_SERVICEBUS_NAMESPACE='….servicebus.windows.net'

java -jar dashflow-api.jar --spring.profiles.active=azure
```

Profile sets `dashflow.platform.broker=servicebus` and excludes RabbitMQ auto-configuration.

## IaC

Bicep entrypoint: [`deploy/azure/main.bicep`](../deploy/azure/main.bicep)

```bash
az deployment group create \
  --resource-group rg-dashflow \
  --template-file deploy/azure/main.bicep \
  --parameters namePrefix=dfdev
```

## Local vs Azure

| | Local | Azure |
|-|-------|-------|
| Profile | `local` | `azure` |
| Broker | RabbitMQ | Service Bus |
| Pipelet env | `IO_BROKER=rabbitmq`, `AMQP_URL` | `IO_BROKER=servicebus`, `SERVICEBUS_CONNECTION_STRING` |

Pipelets use [`pipelets/_common/io_transport.py`](../pipelets/_common/io_transport.py) and select the transport via `IO_BROKER`.
