# plet-rest-source

Source pipelet: HTTP request against a bound **REST** connector (`baseUrl` + step `path`),
emit JSON `payload` and `records` when the body is an array / `{items|records|data}`.

## Build

```bash
docker build -f pipelets/plet-rest-source/Dockerfile -t dashflow/plet-rest-source:local pipelets
```

## Config

| Layer | Required |
|-------|----------|
| Connector (`rest`) | `baseUrl`; optional `timeoutMs`, `api_key`, `headers` |
| Deployment | `region` |
| Execution | `path`; optional `method` (default `GET`), `query`, `headers`, `body`, `timeoutMs` |

## Local run (stdio)

```bash
export CONNECTOR_CONFIG='{"baseUrl":"http://host.docker.internal:4011/api/v3","timeoutMs":30000}'
export DEPLOYMENT_CONFIG='{"region":"us-east-1"}'
export EXECUTION_CONFIG='{"path":"/inventory/items","method":"GET"}'
export IO_MODE=stdio
docker run --rm -e CONNECTOR_CONFIG -e DEPLOYMENT_CONFIG -e EXECUTION_CONFIG -e IO_MODE \
  dashflow/plet-rest-source:local
```

Uses the **petstore-inventory** mock (`:4011`). Full Petstore upload sink remains on `:4010`.

## Tests

```bash
cd pipelets/plet-rest-source && python3 -m unittest test_config.py -v
```
