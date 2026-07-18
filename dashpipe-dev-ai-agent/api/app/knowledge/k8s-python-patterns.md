# Kubernetes Python patterns (Dashpipe pipelets)

## Runtime contract

- Pipelets are batch Jobs (not long-running Deployments) unless the user asks otherwise.
- Read one JSON message from stdin or `INPUT_QUEUE`; write one JSON object to stdout or `OUTPUT_QUEUE`.
- Use `pipelets/_common/io_transport.py` and `config_merge.py` when generating Dashpipe pipelets.
- Environment: `IO_MODE`, `IO_BROKER`, `INPUT_QUEUE`, `OUTPUT_QUEUE`, `AMQP_URL`, `EXECUTION_CONFIG`, `DEPLOYMENT_CONFIG`, `CONNECTOR_CONFIG`, `PIPELET_ID`.

## Dockerfile (slim Python 3.12)

```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY main.py logic.py pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENTRYPOINT ["python", "/app/main.py"]
```

## Job manifest sketch

- `apiVersion: batch/v1`, `kind: Job`
- `restartPolicy: Never`, `backoffLimit: 2`, `ttlSecondsAfterFinished: 600`
- Resource requests: `cpu: 100m`, `memory: 128Mi` for lightweight transforms
- Never embed secrets in manifests; use `envFrom` + Secret or external secret operators

## Design principles

- **SOLID:** separate `logic.py` (pure functions) from `main.py` (I/O shell).
- **DRY:** reuse `_common` helpers; avoid duplicating JSON/env parsing.
- **KISS:** prefer stdlib + minimal deps; log to stderr, data on stdout.

## Health / observability

- Log structured lines to stderr (`log()` from config_merge).
- Expose `/health` only for HTTP services (Deployment), not batch pipelets.
- Use `livenessProbe`/`readinessProbe` with `httpGet` path `/health` on port 8080 when applicable.
