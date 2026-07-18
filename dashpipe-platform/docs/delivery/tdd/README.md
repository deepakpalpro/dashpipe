# TDD documents

Two layers:

| Layer | Audience | Location |
|-------|----------|----------|
| **Wave TDD** | Eng leads, architects, QA, SRE | this folder (`WAVE_N_TDD.md`) |
| **Story TDD** | Developers (junior-friendly playbooks) | [`stories/`](stories/README.md) |

Not support playbooks — those live under [`../kb/`](../kb/).

## Wave TDD (stakeholders)

**Template:** [`../TDD_WAVE_TEMPLATE.md`](../TDD_WAVE_TEMPLATE.md)

| Wave | Document | Status |
|------|----------|--------|
| 0 Foundation | [`WAVE_0_TDD.md`](WAVE_0_TDD.md) | Complete |
| 1 Tenancy / Connectors | [`WAVE_1_TDD.md`](WAVE_1_TDD.md) | Draft |
| 2 Pipelines / Execution | [`WAVE_2_TDD.md`](WAVE_2_TDD.md) | Complete |
| 3 Webhook Ingress | [`WAVE_3_TDD.md`](WAVE_3_TDD.md) | Complete |
| 4 Observability | [`WAVE_4_TDD.md`](WAVE_4_TDD.md) | Complete |
| 5 Metering / PAYG | [`WAVE_5_TDD.md`](WAVE_5_TDD.md) | Complete |
| 6 No-code UI | [`WAVE_6_TDD.md`](WAVE_6_TDD.md) | In Progress |
| 7 Hardening / Ops | [`WAVE_7_TDD.md`](WAVE_7_TDD.md) | Draft |

Update the matching `WAVE_N_TDD.md` when stories ship (red/green evidence, deferrals, exit sign-off). Coverage checkboxes remain in [`../TEST_MATRIX.md`](../TEST_MATRIX.md).

## Story TDD (developers)

**Template:** [`stories/TDD_STORY_TEMPLATE.md`](stories/TDD_STORY_TEMPLATE.md) · **Index:** [`stories/README.md`](stories/README.md)

**Layout:** `stories/w#/W#-US##-tdd.md` (`w0/`, `w1/`, `w2/`, …).  
**Sections:** Overview → Assumptions → HLD/DFD → LLD → API → Testing → Risks → RED → GREEN → REFACTOR → Docs → Common pitfalls.

Waves 0–2 are migrated. For W3+, create `stories/wN/` and copy the template — do not add flat files under `stories/`.