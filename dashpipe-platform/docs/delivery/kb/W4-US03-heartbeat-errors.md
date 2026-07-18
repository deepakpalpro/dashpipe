# KB: Heartbeat + critical errors (Wave 4)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W4-US03 / W4-US03 |
| **Audience** | Platform engineers / SRE / support |
| **Product area** | Observability / Heartbeat & errors |

## Prerequisites

- W4-US01 `PipeletMetricsEmitter`
- W0-US04 `/actuator/prometheus`

## Feature overview

Architecture §7.1 / §7.5 series:

| Metric | Type | Labels |
|--------|------|--------|
| `pipelet_heartbeat_timestamp` | Gauge (epoch **seconds**) | `tenant_id`, `pipeline_id`, `pipelet_id`, `pod_name` |
| `pipelet_errors_total` | Counter | `tenant_id`, `pipeline_id`, `pipelet_id`, `error_type` |

**Cardinality policy**

- `pod_name`: Wave 4 uses fixed stub `stub-pipelet` (not real K8s pod names).
- `error_type`: allowlisted enum only — `processing`, `timeout`, `validation`, `unknown`.
- No `execution_id` on these series.

**Alert rule (document only):** Grafana `time() - pipelet_heartbeat_timestamp > 90` → critical (Notification service out of scope).

**Local stub:** each `StubStageWorker` stage message calls `touchHeartbeat(...)`. Critical errors are recorded via `recordCriticalError(...)` when callers hit failures (no automatic stub failure path).

## How to verify

```bash
./mvnw -pl dashpipe-api test -Dtest=HeartbeatGaugeTest,CriticalErrorCounterTest,PipeletMetricsEmitterTest
```

Manual:

```bash
curl -s localhost:8080/actuator/prometheus | grep -E 'pipelet_heartbeat_timestamp|pipelet_errors_total'
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Stale heartbeat | No stage activity / scrape lag | Confirm stub worker running; alert >90s |
| Missing `pod_name` | Old binary | Redeploy W4-US03+ |
| High `pipelet_errors_total` | Spike by `error_type` | Inspect logs; fix pipelet / input |
| Cardinality explosion | Free-form `error_type` / pod names | Keep enum + stub pod |

## Related

- Developer TDD: [`../tdd/stories/w4/W4-US03-tdd.md`](../tdd/stories/w4/W4-US03-tdd.md)
- Pipelet metrics: [`W4-US01-pipelet-metrics.md`](W4-US01-pipelet-metrics.md)
- Architecture §7.1, §7.5
