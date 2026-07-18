# W4-US01 TDD Guide — Emit pipelet counters + histograms

| Field | Value |
|-------|--------|
| **Story** | W4-US01 — Emit `pipelet_records_*` + processing histograms |
| **Depends on** | W2 fixture run; W0-US04 Prometheus |
| **Branch** | `W4-US01` from `wave-4` |
| **Timebox hint** | 1 day |
| **You will touch** | Micrometer emitter, stub stage/pipelet path, Prometheus IT |
| **Architecture refs** | §7.1 Pipelet Runtime Metrics |
| **KB** | [`../../../kb/W4-US01-pipelet-metrics.md`](../../../kb/W4-US01-pipelet-metrics.md) |
| **Stakeholder TDD** | [`../../WAVE_4_TDD.md`](../../WAVE_4_TDD.md) |
| **AC source** | [`../../../waves/WAVE_4.md`](../../../waves/WAVE_4.md) § W4-US01 |

---

## 1. Overview

Emit Micrometer counters/histograms when a pipelet (or Wave 2 stub stage worker) processes records so Prometheus can scrape them.

**Done means:** `PipeletMetricsEmitterTest` green; `/actuator/prometheus` contains `pipelet_records_in_total` / `pipelet_records_out_total` (and processing histogram) after a fixture emit.

**Out of scope:** Completeness gauge (US02); heartbeat (US03); Grafana (US06).

---

## 2. Assumptions

| # | Assumption |
|---|------------|
| 1 | W0-US04 `/actuator/prometheus` works |
| 2 | W2 stub stage worker or orchestrator can call the emitter |
| 3 | Compose MySQL optional for unit; IT may use Boot only |
| 4 | Prefer low-cardinality labels; document `execution_id` policy |

```bash
git checkout wave-4 && git pull && git checkout -b W4-US01
docker compose up -d mysql rabbitmq   # if IT needs them
```

---

## 3. HLD / DFD

```mermaid
flowchart LR
  Worker[StubStageWorker] -->|records| Emit[PipeletMetricsEmitter]
  Emit --> Micrometer[MeterRegistry]
  Micrometer --> Prom[/actuator/prometheus]
```

Data flow: process N records → increment in/out counters → record duration histogram → scrape.

---

## 4. LLD

| Component | Responsibility |
|-----------|----------------|
| `PipeletMetricsEmitter` | `recordIn`, `recordOut`, `recordProcessing` |
| Metric names | Exact §7.1 names |
| Labels | `tenant_id`, `pipeline_id`, `pipelet_id` (+ `execution_id` only if approved) |
| Wiring | Call from stub worker / orchestrator seam |

---

## 5. API interface

| Surface | Notes |
|---------|--------|
| (No new public REST) | Side effect of processing |
| `GET /actuator/prometheus` | Contains new series after emit |

Auth stub: N/A for scrape; tenant labels from processing context.

---

## 6. Testing

| Layer | Coverage | Tools |
|-------|----------|-------|
| Unit | Emitter increments registry | `PipeletMetricsEmitterTest` |
| Integration | Prometheus body contains names | extend `PrometheusEndpointIT` or new IT |
| Manual | curl prometheus after fixture run | |

---

## 7. Risks

| Risk | Mitigation |
|------|------------|
| Unbounded `execution_id` cardinality | Prefer omit or use low-cardinality id in Wave 4 |
| Name drift vs architecture | Lock §7.1 names in tests |
| Emitting only in real K8s | Stub path must emit in local |

---

## 8. RED

| File | Method | Asserts |
|------|--------|---------|
| `PipeletMetricsEmitterTest` | `emit_incrementsCounters` | in/out + histogram |
| Prometheus IT (extend) | scrape after emit | metric names present |

```bash
./mvnw -pl dashflow-api test -Dtest=PipeletMetricsEmitterTest,PrometheusEndpointIT
```

**Stop.** Red.

---

## 9. GREEN

1. Implement emitter against `MeterRegistry`.
2. Wire into stub processing path.
3. Assert Prometheus scrape.

### Checklist

- [x] `pipelet_records_in_total` / `pipelet_records_out_total`
- [x] Processing histogram present
- [x] Labels include tenant + pipeline + pipelet
- [x] Tests green

---

## 10. REFACTOR

- Shared binder helpers
- Document label policy in KB (`execution_id` omitted)
- Keep emitter free of RabbitMQ details

---

## 11. Docs & trackers

- [x] KB: metric names + how to scrape locally
- [x] Tracker · TEST_MATRIX · `WAVE_4.md` Done

| # | Action | Expected |
|---|--------|----------|
| 1 | Trigger stub process | counters increase |
| 2 | curl `/actuator/prometheus` | series present |

```text
merge → tag W4-US01 → W4-US02
```

---

## 12. Common pitfalls

| Mistake | Fix |
|---------|-----|
| Inventing metric names | Use §7.1 exactly |
| High-cardinality labels | Follow Wave 4 TDD policy |
| Only emitting in Kind | Local stub must emit |

## Help / escalate

- Architecture §7.1 · W0-US04 Prometheus · W2 stub stage worker
