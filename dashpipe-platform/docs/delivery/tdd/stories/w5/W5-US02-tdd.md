# W5-US02 TDD Guide — MeterAgent emit

| Field | Value |
|-------|--------|
| **Story** | W5-US02 — MeterAgent emits from pipelet / stub run |
| **Depends on** | W5-US01; W2 stub stage worker |
| **Branch** | `W5-US02` from `wave-5` |
| **Timebox hint** | 1 day |
| **You will touch** | MeterAgent, stub worker / run path, dimension constants |
| **Architecture refs** | §6.2 Pricing Dimensions |
| **KB** | [`../../../kb/W5-US02-meter-agent.md`](../../../kb/W5-US02-meter-agent.md) |
| **Stakeholder TDD** | [`../../WAVE_5_TDD.md`](../../WAVE_5_TDD.md) |
| **AC source** | [`../../../waves/WAVE_5.md`](../../../waves/WAVE_5.md) § W5-US02 |

---

## 1. Overview

Emit usage events when a fixture pipeline stage runs (records processed, stub compute, optional connector calls) via a `MeterAgent` (or equivalent) into the US01 collector.

**Done means:** `MeterAgentTest` green; fixture run produces expected dimensions in DB/stub.

**Out of scope:** Accurate K8s vCPU from metrics-server; payment.

---

## 2. Assumptions

| # | Assumption |
|---|------------|
| 1 | US01 collector persists (or test double) |
| 2 | Stub stage can emit fixed quantities (like W4 stub records=1) |
| 3 | W3 webhook dimensions remain `platform.webhook_events` / `data.bytes_in` |

```bash
git checkout wave-5 && git pull && git checkout -b W5-US02
```

---

## 3. HLD / DFD

```mermaid
flowchart LR
  Stage[StubStageWorker] --> Agent[MeterAgent]
  Agent --> Coll[UsageEventCollector]
```

---

## 4. LLD

| Component | Responsibility |
|-----------|----------------|
| `MeterAgent` | Map stage/run context → `UsageEvent`s |
| Dimension constants | e.g. `data.records_processed`, stub `compute.vcpu_seconds` |
| Wire into stub worker | After successful stage process |

---

## 5. API interface

| Surface | Notes |
|---------|--------|
| (Internal) | No new public REST required |

---

## 6. Testing

| Layer | Coverage | Tools |
|-------|----------|-------|
| Unit | Known fixture → expected events | `MeterAgentTest` |
| Integration | Run pipeline → events for tenant | optional IT |

---

## 7. Risks

| Risk | Mitigation |
|------|------------|
| Double emit with W4 metrics | Metrics ≠ billing events; document |
| Missing execution_id | Extend `UsageEvent` if needed |

---

## 8. RED

```bash
./mvnw -pl dashpipe-api test -Dtest=MeterAgentTest
```

**Stop.** Red.

---

## 9. GREEN

1. MeterAgent + dimensions.
2. Wire stub worker.
3. Tests green.

### Checklist

- [x] Fixture emit dimensions documented
- [x] Collector receives events
- [x] Tests green

---

## 10. REFACTOR

- Align names with §6.2 table
- Share with US05 tolerance fixture

---

## 11. Docs & trackers

- [x] KB: which dimensions a stub run emits
- [x] Tracker · TEST_MATRIX · `WAVE_5.md` Done

```text
merge → tag W5-US02 → W5-US03
```

---

## 12. Common pitfalls

| Mistake | Fix |
|---------|-----|
| Emitting Prometheus only | Must call usage collector |
| Unbounded custom dimensions | Allowlist constants |

## Help / escalate

- Architecture §6.2 · W5-US01 · W4 stub worker
