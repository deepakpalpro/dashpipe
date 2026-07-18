# KB: Observability REST APIs (Wave 4)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W4-US05 / W4-US05 |
| **Audience** | Support / platform engineers |
| **Product area** | Observability / Admin APIs |

## Prerequisites

- `X-Tenant-Id` header (W1 stub auth)
- W4-US02 completeness on executions
- W4-US03 heartbeat / errors metrics
- W4-US04 log indexer (in-memory stub by default)

## Endpoints

All under `/api/v1/observability`. Cross-tenant → **404**.

| Method | Path | Notes |
|--------|------|--------|
| `GET` | `/pipelines/{id}/completeness` | Latest execution in/out + pct + ratio |
| `GET` | `/pipelines/{id}/latency` | Micrometer processing timer summary (mean/max ms) |
| `GET` | `/pipelines/{id}/heartbeat` | Latest `pipelet_heartbeat_timestamp`; `stale` if >90s |
| `GET` | `/pipelines/{id}/errors` | `pipelet_errors_total` by `error_type` |
| `GET` | `/executions/{execId}/logs` | Log tail from `PipelineLogIndexer` |

## Curl examples

```bash
TENANT=<tenant-uuid>
PIPE=<pipeline-uuid>
EXEC=<execution-uuid>

curl -s -H "X-Tenant-Id: $TENANT" \
  "http://localhost:8080/api/v1/observability/pipelines/$PIPE/completeness"

curl -s -H "X-Tenant-Id: $TENANT" \
  "http://localhost:8080/api/v1/observability/pipelines/$PIPE/heartbeat"

curl -s -H "X-Tenant-Id: $TENANT" \
  "http://localhost:8080/api/v1/observability/executions/$EXEC/logs"
```

## How to verify

```bash
./mvnw -pl dashpipe-api test -Dtest=ObservabilityControllerIT
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 401 | Missing `X-Tenant-Id` | Add header |
| 404 | Wrong tenant / unknown id | Confirm ownership |
| Empty logs | No stub emit yet | Run pipeline; confirm W4-US04 indexer |
| `stale: true` | No recent stage activity | Re-run fixture |

## Related

- Developer TDD: [`../tdd/stories/w4/W4-US05-tdd.md`](../tdd/stories/w4/W4-US05-tdd.md)
- Completeness: [`W4-US02-completeness.md`](W4-US02-completeness.md)
- ELK logs: [`W4-US04-elk-logs.md`](W4-US04-elk-logs.md)
- Architecture §3.6
