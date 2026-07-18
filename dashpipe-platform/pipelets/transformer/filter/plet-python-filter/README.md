# plet-python-filter

Keep records matching a safe expression (`quantity > 0`, `category in ('food','toys')`, …).

**Required:** `deployment.region`, `execution.expression`  
**I/O:** `IO_MODE=stdio` \| `queue` (see [`../REGISTRY.md`](../REGISTRY.md))  
**Input:** `{ "records": [ … ] }`  
**Output:** filtered `records`

```bash
docker build -f pipelets/plet-python-filter/Dockerfile -t dashpipe/plet-python-filter:local pipelets
echo '{"records":[{"quantity":2},{"quantity":0}]}' | docker run --rm -i \
  -e IO_MODE=stdio \
  -e DEPLOYMENT_CONFIG='{"region":"us-east-1"}' \
  -e EXECUTION_CONFIG='{"expression":"quantity > 0"}' \
  dashpipe/plet-python-filter:local
```
