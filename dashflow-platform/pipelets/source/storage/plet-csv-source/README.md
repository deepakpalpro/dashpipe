# plet-csv-source

Source pipelet: fetch a CSV (or other delimited file) from **object storage** via a bound
`storage` connector, parse rows, emit one JSON message with `records` / `recordCount`.

## Build

```bash
docker build -f pipelets/plet-csv-source/Dockerfile -t dashflow/plet-csv-source:local pipelets
```

## Config

| Layer | Required |
|-------|----------|
| Connector (`storage`) | `bucket`; optional `endpoint`, credentials; Azure Blob: `account`, `container`, `endpoint` |
| Deployment | `region` |
| Execution | `objectKey`; optional `delimiter` (default `,`), `hasHeader` (default `true`) |

### Endpoints

| Environment | Typical connector `endpoint` |
|-------------|------------------------------|
| Localdev LocalStack | `http://host.docker.internal:4567` (or Compose service DNS) |
| AKS + LocalStack | in-cluster LocalStack Service URL |
| Azure Blob (S3 API) | `https://<account>.blob.core.windows.net` with compatible creds |

## Local run (stdio)

```bash
export CONNECTOR_CONFIG='{"bucket":"demo-csv-source","endpoint":"http://host.docker.internal:4567","accessKeyId":"test","secretAccessKey":"test"}'
export DEPLOYMENT_CONFIG='{"region":"us-east-1"}'
export EXECUTION_CONFIG='{"objectKey":"inventory/daily.csv","delimiter":",","hasHeader":"true"}'
export IO_MODE=stdio
docker run --rm -e CONNECTOR_CONFIG -e DEPLOYMENT_CONFIG -e EXECUTION_CONFIG -e IO_MODE \
  dashflow/plet-csv-source:local
```

## Tests

```bash
cd pipelets/plet-csv-source && python3 -m unittest test_config.py -v
```
