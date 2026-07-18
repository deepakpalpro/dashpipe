# plet-csv-to-json

Generic CSV → JSON records.

**Required:** `deployment.region`, `execution.delimiter`  
**I/O:** `IO_MODE=stdio` (stdin/stdout) or `queue` (`INPUT_QUEUE` / `OUTPUT_QUEUE` / `AMQP_URL`)  
**Input:** JSON with `csv` or `content`  
**Output:** `records[]`

```bash
docker build -f pipelets/plet-csv-to-json/Dockerfile -t dashflow/plet-csv-to-json:local pipelets
echo '{"csv":"a,b\n1,2"}' | docker run --rm -i \
  -e IO_MODE=stdio \
  -e DEPLOYMENT_CONFIG='{"region":"us-east-1"}' \
  -e EXECUTION_CONFIG='{"delimiter":",","hasHeader":"true"}' \
  dashflow/plet-csv-to-json:local
```
