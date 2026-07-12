# Story TDD guides (developers)

Step-by-step **Red → Green → Refactor** playbooks for juniors implementing a user story.

**Template:** [`TDD_STORY_TEMPLATE.md`](TDD_STORY_TEMPLATE.md)

**Layout (all waves):** `docs/delivery/tdd/stories/w#/W#-US##-tdd.md`

**Section order:** Overview → Assumptions → HLD/DFD → LLD → API → Testing → Risks → RED → GREEN → REFACTOR → Docs → Common pitfalls

Not the same as wave-level stakeholder TDD ([`../README.md`](../README.md)) or support KB ([`../../kb/`](../../kb/)).

## Wave 0

Guides: [`w0/`](w0/)

| Story | Guide | Status |
|-------|-------|--------|
| W0-US01 Compose + LocalStack | [`w0/W0-US01-tdd.md`](w0/W0-US01-tdd.md) | Done (retro guide) |
| W0-US02 Health + MySQL IT | [`w0/W0-US02-tdd.md`](w0/W0-US02-tdd.md) | Done (retro guide) |
| W0-US03 Flyway baseline | [`w0/W0-US03-tdd.md`](w0/W0-US03-tdd.md) | Done (retro guide) |
| W0-US04 Logging + Prometheus | [`w0/W0-US04-tdd.md`](w0/W0-US04-tdd.md) | Done (retro guide) |
| W0-US05 Fixtures + WireMock | [`w0/W0-US05-tdd.md`](w0/W0-US05-tdd.md) | Done (retro guide) |

**Suggested order:** US01 → US02 → US03 → US05 → US04 (matches [`../../waves/WAVE_0.md`](../../waves/WAVE_0.md) delivery sequence).

Execution plan: [`../../waves/WAVE_0.md`](../../waves/WAVE_0.md).  
Stakeholder wave TDD: [`../WAVE_0_TDD.md`](../WAVE_0_TDD.md).

## Wave 1

Guides: [`w1/`](w1/)

| Story | Guide | Status |
|-------|-------|--------|
| W1-US01 Tenant CRUD + context | [`w1/W1-US01-tdd.md`](w1/W1-US01-tdd.md) | Done (first impl on `W1-US01`) |
| W1-US02 Tenant isolation filters | [`w1/W1-US02-tdd.md`](w1/W1-US02-tdd.md) | Done (impl on `W1-US02`) |
| W1-US03 Service types + defaults | [`w1/W1-US03-tdd.md`](w1/W1-US03-tdd.md) | Done |
| W1-US04 Tenant service config | [`w1/W1-US04-tdd.md`](w1/W1-US04-tdd.md) | Done |
| W1-US05 Connector SPI + Rest | [`w1/W1-US05-tdd.md`](w1/W1-US05-tdd.md) | Done |
| W1-US06 Connector test WireMock | [`w1/W1-US06-tdd.md`](w1/W1-US06-tdd.md) | Done |
| W1-US07 Storage LocalStack S3 | [`w1/W1-US07-tdd.md`](w1/W1-US07-tdd.md) | Done |
| W1-US08 MessageBus LocalStack SQS | [`w1/W1-US08-tdd.md`](w1/W1-US08-tdd.md) | Done |

**Suggested order:** US01 → US02 → US03 → US04 → US05 → US06 → US07 → US08.  
US05 can start after W0-US05 even while US03/US04 are in flight if staffing allows — but **do not skip US02** before tenant-owned connector APIs.

Execution plan (full AC): [`../../waves/WAVE_1.md`](../../waves/WAVE_1.md).  
Stakeholder wave TDD: [`../WAVE_1_TDD.md`](../WAVE_1_TDD.md).

## Wave 2

Guides: [`w2/`](w2/)

| Story | Guide | Status |
|-------|-------|--------|
| W2-US01 Pipeline CRUD | [`w2/W2-US01-tdd.md`](w2/W2-US01-tdd.md) | Done |
| W2-US02 Pipeline steps | [`w2/W2-US02-tdd.md`](w2/W2-US02-tdd.md) | Done |
| W2-US03 RabbitMQ topology | [`w2/W2-US03-tdd.md`](w2/W2-US03-tdd.md) | Done |
| W2-US04 Async run | [`w2/W2-US04-tdd.md`](w2/W2-US04-tdd.md) | Done |
| W2-US05 Pipelet Job spawn | [`w2/W2-US05-tdd.md`](w2/W2-US05-tdd.md) | Done |
| W2-US06 Retries + DLQ | [`w2/W2-US06-tdd.md`](w2/W2-US06-tdd.md) | Done |
| W2-US07 Execution status | [`w2/W2-US07-tdd.md`](w2/W2-US07-tdd.md) | Done |

**Suggested order:** US01 → US02 → US03 → US04 → US05 → US06 → US07 (US06 can start after US03).

Execution plan (full AC): [`../../waves/WAVE_2.md`](../../waves/WAVE_2.md).  
Stakeholder wave TDD: [`../WAVE_2_TDD.md`](../WAVE_2_TDD.md).

## Wave 3

Guides: [`w3/`](w3/)

| Story | Guide | Status |
|-------|-------|--------|
| W3-US01 Ingress accept + publish | [`w3/W3-US01-tdd.md`](w3/W3-US01-tdd.md) | Done |
| W3-US02 Signature + Auth | [`w3/W3-US02-tdd.md`](w3/W3-US02-tdd.md) | Done |
| W3-US03 Idempotency | [`w3/W3-US03-tdd.md`](w3/W3-US03-tdd.md) | Done |
| W3-US04 Rate limit / 503 | [`w3/W3-US04-tdd.md`](w3/W3-US04-tdd.md) | Done |
| W3-US05 Provision webhook URL | [`w3/W3-US05-tdd.md`](w3/W3-US05-tdd.md) | Done |
| W3-US06 On-demand processor | [`w3/W3-US06-tdd.md`](w3/W3-US06-tdd.md) | Done |
| W3-US07 Meter webhook events | [`w3/W3-US07-tdd.md`](w3/W3-US07-tdd.md) | Done |

**Suggested order:** US01 → US02 → US03 → US05 → US06 → US07 (US04 Should can parallel after US01).

Execution plan (full AC): [`../../waves/WAVE_3.md`](../../waves/WAVE_3.md).  
Stakeholder wave TDD: [`../WAVE_3_TDD.md`](../WAVE_3_TDD.md).

## Wave 4

Guides: [`w4/`](w4/)

| Story | Guide | Status |
|-------|-------|--------|
| W4-US01 Pipelet metrics emit | [`w4/W4-US01-tdd.md`](w4/W4-US01-tdd.md) | Done |
| W4-US02 Completeness ratio | [`w4/W4-US02-tdd.md`](w4/W4-US02-tdd.md) | Done |
| W4-US03 Heartbeat + errors | [`w4/W4-US03-tdd.md`](w4/W4-US03-tdd.md) | Done |
| W4-US04 ELK log path | [`w4/W4-US04-tdd.md`](w4/W4-US04-tdd.md) | Done |
| W4-US05 Observability REST | [`w4/W4-US05-tdd.md`](w4/W4-US05-tdd.md) | Done |
| W4-US06 Grafana provision | [`w4/W4-US06-tdd.md`](w4/W4-US06-tdd.md) | Done |

**Suggested order:** US01 → US02 → US03 → US04 → US05 (US06 Should can follow US02).

Execution plan (full AC): [`../../waves/WAVE_4.md`](../../waves/WAVE_4.md).  
Stakeholder wave TDD: [`../WAVE_4_TDD.md`](../WAVE_4_TDD.md).

## Wave 5

Guides: [`w5/`](w5/)

| Story | Guide | Status |
|-------|-------|--------|
| W5-US01 UsageEvent ingest | [`w5/W5-US01-tdd.md`](w5/W5-US01-tdd.md) | Done |
| W5-US02 MeterAgent emit | [`w5/W5-US02-tdd.md`](w5/W5-US02-tdd.md) | Done |
| W5-US03 Hourly aggregates | [`w5/W5-US03-tdd.md`](w5/W5-US03-tdd.md) | Done |
| W5-US04 Quota + credits | [`w5/W5-US04-tdd.md`](w5/W5-US04-tdd.md) | Done |
| W5-US05 Usage/billing APIs | [`w5/W5-US05-tdd.md`](w5/W5-US05-tdd.md) | Done |
| W5-US06 Block run 402 | [`w5/W5-US06-tdd.md`](w5/W5-US06-tdd.md) | Done |

**Suggested order:** US01 → US02 → US03 → US04 → US05 → US06 (US05 can parallel US04 after US03).

Execution plan (full AC): [`../../waves/WAVE_5.md`](../../waves/WAVE_5.md).  
Stakeholder wave TDD: [`../WAVE_5_TDD.md`](../WAVE_5_TDD.md).

## Wave 6

Guides: [`w6/`](w6/)

| Story | Guide | Status |
|-------|-------|--------|
| W6-US01 Nav shell + tenant context | [`w6/W6-US01-tdd.md`](w6/W6-US01-tdd.md) | Done |
| W6-US02 Connectors / Services UI | [`w6/W6-US02-tdd.md`](w6/W6-US02-tdd.md) | Done |
| W6-US03 Pipelets catalog | [`w6/W6-US03-tdd.md`](w6/W6-US03-tdd.md) | Done |
| W6-US04 Pipeline builder | [`w6/W6-US04-tdd.md`](w6/W6-US04-tdd.md) | Done |
| W6-US05 Run / overlay | [`w6/W6-US05-tdd.md`](w6/W6-US05-tdd.md) | Done |
| W6-US06 Observability panels | [`w6/W6-US06-tdd.md`](w6/W6-US06-tdd.md) | Done |

**Suggested order:** US01 → US02 → US03 → US04 → US05 → US06 (US02 can parallel US03 after US01).

Execution plan (full AC): [`../../waves/WAVE_6.md`](../../waves/WAVE_6.md).  
Stakeholder wave TDD: [`../WAVE_6_TDD.md`](../WAVE_6_TDD.md).

## Later waves (W7+)

1. Create folder `docs/delivery/tdd/stories/wN/` (e.g. `w7/`).
2. Copy [`TDD_STORY_TEMPLATE.md`](TDD_STORY_TEMPLATE.md) to `wN/W#-US##-tdd.md`.
3. Fill all 12 sections **before** coding.
4. Link from `waves/WAVE_N.md`, `tdd/WAVE_N_TDD.md`, the story KB, and this README.

Use `w0/`–`w6/` guides as the pattern. Do **not** put new guides in the flat `stories/` root.
