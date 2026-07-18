# Wave Tracker

Track delivery progress across waves. Update status as stories move. Parent plan: [`../DELIVERY_PLAN.md`](../DELIVERY_PLAN.md).

**TDD (technical stakeholders):** [`tdd/README.md`](tdd/README.md) · template [`TDD_WAVE_TEMPLATE.md`](TDD_WAVE_TEMPLATE.md)

**Status values:** `Todo` | `In Progress` | `Blocked` | `Done`

**Test gate:** leave blank until WIP; when Done, list completed gates (e.g. `U,I,WM,LS,M,KB`).

Abbreviations: **U** = Unit, **I** = Integration, **WM** = WireMock, **LS** = LocalStack, **M** = Manual, **KB** = Support article

---

## Wave 0 — Foundation

**Wave goal:** `docker compose up` + health API; tests green.  
**Plan:** [`waves/WAVE_0.md`](waves/WAVE_0.md) · **TDD:** [`tdd/WAVE_0_TDD.md`](tdd/WAVE_0_TDD.md) · **Branch:** `wave-0`  
**KB:** [`kb/W0-US01-local-compose-stack.md`](kb/W0-US01-local-compose-stack.md) · [`kb/W0-US02-health-endpoint.md`](kb/W0-US02-health-endpoint.md) · [`kb/W0-US03-flyway-baseline.md`](kb/W0-US03-flyway-baseline.md) · [`kb/W0-US04-logging-prometheus.md`](kb/W0-US04-logging-prometheus.md) · [`kb/W0-US05-mock-data-wiremock.md`](kb/W0-US05-mock-data-wiremock.md)

| Story ID | Feature / Epic | Title | Status | Owner | Test gate | Blockers |
|----------|----------------|-------|--------|-------|-----------|----------|
| W0-US01 | W0-F1 / W0-F1-E1 | Compose stack + LocalStack healthy | Done | | LS,M,KB | Verified on host port 4567 |
| W0-US02 | W0-F1 / W0-F1-E2 | Spring Boot health + Compose MySQL IT | Done | | U,I,M,KB | Rancher Desktop: Compose MySQL IT (Testcontainers blocked by docker-java API) |
| W0-US03 | W0-F1 / W0-F1-E2 | Flyway baseline schema apply | Done | | U,I,M,KB | |
| W0-US04 | W0-F1 / W0-F1-E3 | Structured logging + Micrometer smoke | Done | | U,I,M,KB | |
| W0-US05 | W0-F1 / W0-F1-E4 | Mock-data factories + WireMock harness | Done | | U,WM,M,KB | |

**Wave exit criteria:** All Must stories Done; TEST_MATRIX rows checked for Wave 0.

---

## Wave 1 — Tenancy, Services, Connectors

**Wave goal:** Tenant + service config + Rest connector test against WireMock/LocalStack.  
**Plan:** [`waves/WAVE_1.md`](waves/WAVE_1.md) · **TDD:** [`tdd/WAVE_1_TDD.md`](tdd/WAVE_1_TDD.md) · **Developer guides:** [`tdd/stories/README.md`](tdd/stories/README.md) § Wave 1  
**Branch:** `wave-1`  
**KB:** [`kb/W1-US01-tenant-crud-context.md`](kb/W1-US01-tenant-crud-context.md) · [`kb/W1-US02-tenant-isolation.md`](kb/W1-US02-tenant-isolation.md) · [`kb/W1-US03-service-types.md`](kb/W1-US03-service-types.md) · [`kb/W1-US04-tenant-service-config.md`](kb/W1-US04-tenant-service-config.md) · [`kb/W1-US05-connector-spi.md`](kb/W1-US05-connector-spi.md) · [`kb/W1-US06-connector-test-wiremock.md`](kb/W1-US06-connector-test-wiremock.md) · [`kb/W1-US07-storage-localstack.md`](kb/W1-US07-storage-localstack.md) · [`kb/W1-US08-messagebus-sqs.md`](kb/W1-US08-messagebus-sqs.md)

| Story ID | Feature / Epic | Title | Status | Owner | Test gate | Blockers |
|----------|----------------|-------|--------|-------|-----------|----------|
| W1-US01 | W1-F1 / W1-F1-E1 | Tenant CRUD + JWT tenant context | Done | | U,I,M,KB | Stub `X-Tenant-Id` (JWT later) |
| W1-US02 | W1-F1 / W1-F1-E1 | Tenant isolation filters (JPA) | Done | | U,I,M,KB | Proven via `tenant_notes` + Hibernate filter |
| W1-US03 | W1-F2 / W1-F2-E1 | Service types + defaults | Done | | U,I,M,KB | Seeded `StubAuth` via Flyway V3 |
| W1-US04 | W1-F2 / W1-F2-E1 | Tenant service config (Auth pattern) | Done | | U,I,M,KB | Crypto stub `encrypted:` prefix; redaction on responses |
| W1-US05 | W1-F3 / W1-F3-E1 | Connector SPI load + Rest plugin | Done | | U,I,M,KB | Spring bean registry (PF4J deferred) |
| W1-US06 | W1-F3 / W1-F3-E1 | Connector test vs WireMock | Done | | U,I,WM,M,KB | |
| W1-US07 | W1-F3 / W1-F3-E2 | Storage connector vs LocalStack S3 | Done | | U,I,LS,M,KB | |
| W1-US08 | W1-F3 / W1-F3-E2 | MessageBus connector vs LocalStack SQS | Done | | U,I,LS,M,KB | Should priority — completed |

**Wave exit criteria:** Rest + Storage + MessageBus paths proven with mocks; no cross-tenant connector read.

---

## Wave 2 — Pipelines & Ephemeral Execution

**Wave goal:** Source → Processor → Destination via RabbitMQ; execution status in MySQL.  
**Plan:** [`waves/WAVE_2.md`](waves/WAVE_2.md) · **TDD:** [`tdd/WAVE_2_TDD.md`](tdd/WAVE_2_TDD.md) · **Developer guides:** [`tdd/stories/README.md`](tdd/stories/README.md) § Wave 2  
**Branch:** `wave-2`  
**KB:** [`kb/W2-US01-pipeline-crud.md`](kb/W2-US01-pipeline-crud.md) · [`kb/W2-US02-pipeline-steps.md`](kb/W2-US02-pipeline-steps.md) · [`kb/W2-US03-rabbit-topology.md`](kb/W2-US03-rabbit-topology.md) · [`kb/W2-US04-async-run.md`](kb/W2-US04-async-run.md) · [`kb/W2-US05-pipelet-job.md`](kb/W2-US05-pipelet-job.md) · [`kb/W2-US06-stage-dlq.md`](kb/W2-US06-stage-dlq.md) · [`kb/W2-US07-execution-status.md`](kb/W2-US07-execution-status.md)

| Story ID | Feature / Epic | Title | Status | Owner | Test gate | Blockers |
|----------|----------------|-------|--------|-------|-----------|----------|
| W2-US01 | W2-F1 / W2-F1-E1 | Pipeline CRUD + visibility/mode | Done | | U,I,M,KB | DELETE archives (soft) |
| W2-US02 | W2-F1 / W2-F1-E1 | Pipeline steps config API | Done | | U,I,M,KB | Full replace; empty rejected |
| W2-US03 | W2-F2 / W2-F2-E1 | Inter-stage RabbitMQ topology | Done | | U,I,M,KB | Tenant-prefixed; idempotent declare |
| W2-US04 | W2-F2 / W2-F2-E2 | Async run orchestration | Done | | U,I,M,KB | Stub worker completes stages |
| W2-US05 | W2-F2 / W2-F2-E2 | Pipelet Job spawn (Kind/stub) | Done | | U,I,M,KB | Stub records creates; Kind optional |
| W2-US06 | W2-F3 / W2-F3-E1 | Retries + per-stage DLQ | Done | | U,I,M,KB | DLX + RetryPolicy; poison IT |
| W2-US07 | W2-F1 / W2-F1-E2 | Execution status query API | Done | | U,I,M,KB | List + detail; ExecutionStatusIT |

**Wave exit criteria:** Fixture 3-stage pipeline completes; DLQ path exercised once.  
**Exit verified:** 2026-07-09 — `PipelineRunIT` + `ExecutionStatusIT` + `StageDlqIT` (+ topology/CRUD ITs) green. Tag `wave-2-complete`.

---

## Wave 3 — Webhook Ingress + Queue

**Wave goal:** External POST `202`; event on tenant webhook queue; processing without ingress cold-start.  
**Plan:** [`waves/WAVE_3.md`](waves/WAVE_3.md) · **TDD:** [`tdd/WAVE_3_TDD.md`](tdd/WAVE_3_TDD.md) · **Developer guides:** [`tdd/stories/README.md`](tdd/stories/README.md) § Wave 3  
**Branch:** `wave-3`

| Story ID | Feature / Epic | Title | Status | Owner | Test gate | Blockers |
|----------|----------------|-------|--------|-------|-----------|----------|
| W3-US01 | W3-F1 / W3-F1-E1 | Ingress accept + queue publish | Done | | U,I,M,KB | 202 + queue; no Job on accept |
| W3-US02 | W3-F1 / W3-F1-E1 | Signature verification + auth service | Done | | U,I,M,KB | HMAC before publish; 401 on bad sig |
| W3-US03 | W3-F1 / W3-F1-E2 | Idempotency (X-Webhook-Id / hash) | Done | | U,I,M,KB | Same event_id; single publish; V13 store |
| W3-US04 | W3-F1 / W3-F1-E2 | Rate limit + backpressure 429/503 | Done | | U,I,M,KB | Per-tenant 429; publish fail → 503 |
| W3-US05 | W3-F2 / W3-F2-E1 | Provision webhook URL API | Done | | U,I,M,KB | Stable URL; encrypted secret; event_listener only |
| W3-US06 | W3-F2 / W3-F2-E1 | On-demand processor trigger (queue depth) | Done | | U,I,M,KB | Poller + stub Job; coalesce; ingress no Job |
| W3-US07 | W3-F3 / W3-F3-E1 | Meter webhook_events + bytes_in | Done | | U,I,M,KB | Emit once per logical event; stub collector |

**Wave exit criteria:** Support KB for webhook troubleshooting published; architecture §11 behaviors covered.

---

## Wave 4 — Observability

**Wave goal:** Completeness % and logs visible for a known fixture execution.  
**Plan:** [`waves/WAVE_4.md`](waves/WAVE_4.md) · **TDD:** [`tdd/WAVE_4_TDD.md`](tdd/WAVE_4_TDD.md) · **Developer guides:** [`tdd/stories/README.md`](tdd/stories/README.md) § Wave 4  
**Branch:** `wave-4` · **Tags:** `W4-US01`–`W4-US06`, `wave-4-complete` · **PR:** [#9](https://github.com/deepakpalpro/dashpipe/pull/9) → `master` (**merged**)

| Story ID | Feature / Epic | Title | Status | Owner | Test gate | Blockers |
|----------|----------------|-------|--------|-------|-----------|----------|
| W4-US01 | W4-F1 / W4-F1-E1 | Emit pipelet counters + histograms | Done | | U,I,M,KB | §7.1 names; no execution_id label |
| W4-US02 | W4-F1 / W4-F1-E1 | Completeness metric on fixture run | Done | | U,I,M,KB | §7.4; gauge labels tenant+pipeline only |
| W4-US03 | W4-F1 / W4-F1-E2 | Heartbeat + critical error metrics | Done | | U,M,KB | §7.5 epoch seconds; stub pod; error_type enum |
| W4-US04 | W4-F2 / W4-F2-E1 | Logstash → ES → Kibana index pattern | Done | | U,M,KB | Stub indexer CI; compose --profile elk optional |
| W4-US05 | W4-F2 / W4-F2-E2 | Observability REST APIs | Done | | U,I,M,KB | /api/v1/observability; cross-tenant 404 |
| W4-US06 | W4-F2 / W4-F2-E1 | Grafana dashboard provisioning (tenant) | Done | | U,M,KB | Should; StubGrafanaClient + provisioner |

**Wave exit criteria:** Support can locate completeness and error logs for fixture `exec-*`. **Met** (US02 completeness + US04/US05 logs; tag `wave-4-complete`; PR #9 merged).

---

## Wave 5 — Metering & Pay-as-you-go

**Wave goal:** Fixture run yields billable events across compute, records, connector calls, webhooks.  
**Plan:** [`waves/WAVE_5.md`](waves/WAVE_5.md) · **TDD:** [`tdd/WAVE_5_TDD.md`](tdd/WAVE_5_TDD.md) · **Developer guides:** [`tdd/stories/README.md`](tdd/stories/README.md) § Wave 5  
**Branch:** `wave-5` · **Tags:** `W5-US01`–`W5-US06`, `wave-5-complete` · **PR:** [#10](https://github.com/deepakpalpro/dashpipe/pull/10) → `master` (**merged**)

| Story ID | Feature / Epic | Title | Status | Owner | Test gate | Blockers |
|----------|----------------|-------|--------|-------|-----------|----------|
| W5-US01 | W5-F1 / W5-F1-E1 | UsageEvent ingest + persist | Done | | U,I,M,KB | V14 usage_events; PersistingUsageEventCollector |
| W5-US02 | W5-F1 / W5-F1-E1 | MeterAgent emit from pipelet sidecar/lib | Done | | U,I,M,KB | Stub records+vcpu; pipeline_runs on last stage |
| W5-US03 | W5-F1 / W5-F1-E2 | Hourly aggregates job | Done | | U,I,M,KB | V15 usage_aggregates; UTC hourly upsert + stub cost |
| W5-US04 | W5-F2 / W5-F2-E1 | Quota soft/hard + credit balance | Done | | U,I,M,KB | QuotaEvaluator; credit deduct on aggregate delta |
| W5-US05 | W5-F2 / W5-F2-E1 | Usage and billing query APIs | Done | | U,I,M,KB | §3.5 usage/events/quota/periods; cross-tenant 404 |
| W5-US06 | W5-F2 / W5-F2-E2 | Block run on hard limit / zero credit (402) | Done | | U,I,M,KB | Pre-run QuotaService gate; HTTP 402; no execution row |

**Wave exit criteria:** Usage summary matches fixture meters within tolerance; hard/zero credit returns `402`; billing-dispute KB drafted. **Met** (US05 tolerance + US06 `RunBlockedIT`; tag `wave-5-complete`).

---

## Wave 6 — No-code UI

**Wave goal:** Build and run a 3-step pipeline in UI without code.  
**Plan:** [`waves/WAVE_6.md`](waves/WAVE_6.md) · **TDD:** [`tdd/WAVE_6_TDD.md`](tdd/WAVE_6_TDD.md) · **Developer guides:** [`tdd/stories/README.md`](tdd/stories/README.md) § Wave 6  
**Branch:** `wave-6`

| Story ID | Feature / Epic | Title | Status | Owner | Test gate | Blockers |
|----------|----------------|-------|--------|-------|-----------|----------|
| W6-US01 | W6-F1 / W6-F1-E1 | Level-1 nav shell + auth context | Done | | AuthContext.test + Shell.test | Billing nav added |
| W6-US02 | W6-F1 / W6-F1-E2 | Connectors / Services list+forms | Done | | ConnectorForm/ServiceForm + list/detail MSW + filter/pagination | Dual config + V18/V19 seed |
| W6-US03 | W6-F1 / W6-F1-E2 | Global Pipelets catalog + admin register | Done | | catalogFilter + PipeletsCatalog | ~105 fixture + dual defaults |
| W6-US04 | W6-F2 / W6-F2-E1 | Drag-drop pipeline builder save | Done | | pipelineGraphReducer + PipelineBuilder.save + list page | Searchable connector select |
| W6-US05 | W6-F2 / W6-F2-E1 | Run / dry-run / execution overlay | Done | | executionOverlay + RunControls.quota | API dry-run endpoint |
| W6-US06 | W6-F2 / W6-F2-E2 | Observability panels in UI | Done | | Completeness/Latency/Heartbeat panels | |

**Wave exit criteria:** Manual E2E script passes without Postman for happy path.  
**Exit verified:** 2026-07-10 — UI suite green; dual-config + seed docs; PR `wave-6` → `master`. Tag `wave-6-complete`.

---

## Wave 7 — Hardening & Ops

**Wave goal:** Production-readiness checklist + support playbooks for major features.

| Story ID | Feature / Epic | Title | Status | Owner | Test gate | Blockers |
|----------|----------------|-------|--------|-------|-----------|----------|
| W7-US01 | W7-F1 / W7-F1-E1 | Sync execution mode | Todo | | | |
| W7-US02 | W7-F1 / W7-F1-E2 | Cross-tenant access denied suite | Todo | | | |
| W7-US03 | W7-F1 / W7-F1-E2 | Pipeline version rollback | Todo | | | |
| W7-US04 | W7-F2 / W7-F2-E1 | ResourceQuota / NetworkPolicy verified | Todo | | | |
| W7-US05 | W7-F2 / W7-F2-E2 | Support KB final suite (waves 1–6) | Todo | | | |
| W7-US06 | W7-F2 / W7-F2-E2 | Production readiness checklist sign-off | Todo | | | |

**Wave exit criteria:** All Must stories Done; TEST_MATRIX complete for shipped waves; Go/No-Go signed.

---

## Story branch lifecycle

After each user story ships (example ids: `W0-US02`, wave branch `wave-0`):

1. Merge feature branch into the wave branch (`git merge --no-ff W0-USnn`).
2. Create annotated tag on the wave tip with the **story id** (`git tag -a W0-USnn -m "..."`).
3. Push wave branch + tag (`git push origin wave-0` and `git push origin refs/tags/W0-USnn`).
4. Delete feature branch local and remote (`git branch -d W0-USnn` · `git push origin --delete refs/heads/W0-USnn`).
5. Create and push the next story branch from the wave branch (`git checkout -b W0-USnn+1` · `git push -u origin HEAD`).

Canonical description: [`../DELIVERY_PLAN.md`](../DELIVERY_PLAN.md) → Working agreements §6.

---

## Summary counts

| Wave | Todo | In Progress | Blocked | Done |
|------|------|-------------|---------|------|
| 0 | | | | |
| 1 | | | | 8 |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |
| 6 | | | | |
| 7 | | | | |
