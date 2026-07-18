# KB: Observability panels (Wave 6)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W6-US06 / W6-US06 |
| **Audience** | Frontend engineers / support |
| **Product area** | UI observability |
| **Priority** | Should |

## Prerequisites

- W4 observability REST
- W6-US01 Observability nav route
- Optional MSW: `VITE_ENABLE_MSW=true`

## Feature overview

`/observability` tabs: Completeness | Latency | Heartbeat | Critical Errors (stub).

| Control | Behaviour |
|---------|-----------|
| Pipeline selector | From `GET /api/v1/pipelines` |
| Time range | `1h` / `24h` / `7d` (query key; BFF may ignore until W4 supports range) |
| Completeness | `%`, records in/out, warn if &lt; 95% |
| Latency | mean/max + p50/p95 labels |
| Heartbeat | last-seen ISO timestamp + Healthy/Stale |

### Mock shapes (MSW)

```json
{ "completeness_pct": 98, "records_in": 1000, "records_out": 980 }
{ "p95_ms": 120, "mean_ms": 48.5, "sample_count": 42 }
{ "last_heartbeat_epoch_seconds": 1720000000, "stale": false }
```

## How to verify

```bash
cd dashflow-ui
npm test -- CompletenessPanel LatencyPanel HeartbeatPanel
```

Manual: open Observability → pick pipeline → switch tabs; confirm metrics render.

## Related

- Developer TDD: [`../tdd/stories/w6/W6-US06-tdd.md`](../tdd/stories/w6/W6-US06-tdd.md)
- W4-US05 · Architecture §4.6–§4.7
