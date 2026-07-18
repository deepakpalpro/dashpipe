# plet-field-mapper

Rename/project fields; optional wrap for bulk sinks.

**Required:** `deployment.region`, `execution.mode`  
**I/O:** `IO_MODE=stdio` \| `queue` (see [`../REGISTRY.md`](../REGISTRY.md))  
**Execution:** `mapping` (`a=b,c=d` or JSON), `mode` = `map` \| `upsert` \| `replace`

```bash
docker build -f pipelets/plet-field-mapper/Dockerfile -t dashflow/plet-field-mapper:local pipelets
echo '{"records":[{"unit_price":1,"sku":"A"}]}' | docker run --rm -i \
  -e IO_MODE=stdio \
  -e DEPLOYMENT_CONFIG='{"region":"us-east-1"}' \
  -e EXECUTION_CONFIG='{"mode":"upsert","mapping":"unit_price=unitPrice,sku=sku"}' \
  dashflow/plet-field-mapper:local
```
