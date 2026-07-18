# Local Development

Guide to developing and debugging Dashpipe on your machine.

## Standard local stack

```bash
./dashpipe-ci_cd/scripts/localdev.sh start              # Compose + API + UI
./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics
./dashpipe-ci_cd/scripts/localdev.sh start --with-elk
./dashpipe-ci_cd/scripts/localdev.sh status
./dashpipe-ci_cd/scripts/localdev.sh logs -f
```

## Real pipelet execution (Kubernetes)

To run actual pipelet containers instead of the API stub worker:

```bash
./dashpipe-ci_cd/scripts/localdev.sh start --k8s --with-metrics
```

This:

- Switches API to Spring profiles `local,k8s`
- Builds pipelet Docker images (`dashpipe/plet-*:local`)
- Creates K8s Jobs in namespace `tenant-t001` when you **Run** a pipeline

**Important:** UI status "completed" alone does not prove pipelets ran. Verify K8s Jobs and downstream systems (e.g. Petstore inventory).

Full walkthrough: [LOCALDEV_PIPELINE_GUIDE.md](../../dashpipe-platform/docs/LOCALDEV_PIPELINE_GUIDE.md)

## Build pipelet images only

```bash
./dashpipe-ci_cd/scripts/localdev.sh build-pipelets
```

Override which pipelets to build:

```bash
PIPELET_IDS="plet-s3-source plet-csv-source plet-webhook-destination" \
  ./dashpipe-ci_cd/scripts/localdev.sh build-pipelets
```

## Regenerate pipelet catalog fixture

After adding or changing pipelets under `dashpipe-platform/pipelets/`:

```bash
python3 dashpipe-ci_cd/scripts/generate_catalog_pipelets.py
```

Updates `dashpipe-platform/dashpipe-ui/src/fixtures/pipelets.json`.

## Environment overrides

| Variable | Default | Purpose |
|----------|---------|---------|
| `TENANT_ID` | `T001` | Tenant header for API calls |
| `API_URL` | `http://localhost:8080` | API base URL |
| `UI_URL` | `http://127.0.0.1:5173` | UI dev server |
| `S3_ENDPOINT` | `http://localhost:4567` | LocalStack |
| `PIPELET_IDS` | all catalog pipelets | Subset for image builds |

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| API health 503 | Wait for MySQL; check `dashpipe-ci_cd/scripts/localdev.sh logs` |
| ImagePullBackOff on Jobs | Run `build-pipelets` |
| Grafana empty | Confirm API exposes `/actuator/prometheus`; run smoke-metrics |
| Port conflicts | Stop other Compose stacks using 3306/8080/5173 |

See the troubleshooting table in [LOCALDEV_PIPELINE_GUIDE.md](../../dashpipe-platform/docs/LOCALDEV_PIPELINE_GUIDE.md).
