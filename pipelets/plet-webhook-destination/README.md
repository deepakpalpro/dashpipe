# plet-webhook-destination

POST JSON to `connector.baseUrl` + `execution.path`.

**Required:** `deployment.region`, `execution.path`  
**Connector:** `baseUrl` (REST)  
**I/O:** `IO_MODE=stdio` \| `queue` (see [`../REGISTRY.md`](../REGISTRY.md))  
**Body:** `petstorePayload` / `payload` / `records` (auto-wrapped)

```bash
docker build -f pipelets/plet-webhook-destination/Dockerfile -t dashflow/plet-webhook-destination:local pipelets
echo '{"payload":{"mode":"upsert","items":[]}}' | docker run --rm -i --network dashflow_default \
  -e IO_MODE=stdio \
  -e CONNECTOR_CONFIG='{"baseUrl":"http://petstore:4010/api/v3"}' \
  -e DEPLOYMENT_CONFIG='{"region":"us-east-1"}' \
  -e EXECUTION_CONFIG='{"path":"/inventory/upload","method":"POST"}' \
  dashflow/plet-webhook-destination:local
```
