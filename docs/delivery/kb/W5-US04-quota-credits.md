# KB: Quota + credits (Wave 5)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W5-US04 / W5-US04 |
| **Audience** | Platform engineers / billing / support |
| **Product area** | Metering / Quota |

## Prerequisites

- W5-US03 hourly aggregates
- `tenants.credit_balance` + `quota_config` (V1 baseline)

## Feature overview

| Piece | Role |
|-------|------|
| `QuotaEvaluator` | Pure decision: ALLOW / SOFT_WARN / HARD_BLOCK / NO_CREDIT |
| `QuotaConfigParser` | Parse `tenants.quota_config` JSON |
| `QuotaService` | Load tenant + month usage → evaluate; soft → log warn |
| `CreditBalanceService` | Deduct / set / read `credit_balance` |
| Aggregate hook | `UsageAggregateService` deducts **cost delta** on upsert |

### Decision priority

1. **NO_CREDIT** — `credit_balance ≤ 0` (blocks run; US06 → 402)
2. **HARD_BLOCK** — any dimension usage ≥ hard (blocks run)
3. **SOFT_WARN** — any dimension usage ≥ soft and &lt; hard (allows run; log only)
4. **ALLOW** — otherwise

Soft **never** blocks. Hard / zero credit **do** (HTTP wiring in W5-US06).

### `quota_config` schema

```json
{
  "dimensions": {
    "platform.pipeline_runs": { "soft": 100, "hard": 200 },
    "data.records_processed": { "soft": 1000000, "hard": 2000000 }
  }
}
```

- Omit a dimension → no limit for that dimension.
- Omit `soft` or `hard` → that side unchecked.
- Invalid / null JSON → empty config (credit check only).

### Credit deduct

On hourly aggregate insert: deduct full `total_cost`.  
On re-run upsert: deduct `(new_cost - previous_cost)` only (idempotent).

New tenants default to `credit_balance = 0` — top up before expecting ALLOW. After W5-US06, **new** tenants get trial credit **100.0000** on create (`TenantService.DEFAULT_CREDIT_BALANCE`). Legacy rows may still be 0.

## How to verify

```bash
./mvnw -pl dashflow-api test -Dtest=QuotaEvaluatorTest,CreditBalanceServiceTest,CreditBalanceIT
```

```sql
SELECT id, credit_balance, quota_config FROM tenants WHERE id = '<tenant-uuid>';
```

Set credit / config (support / IT):

```text
CreditBalanceService.setBalance(tenantId, new BigDecimal("100.0000"));
-- or UPDATE tenants SET credit_balance = 100, quota_config = '...' WHERE id = ...
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Always NO_CREDIT | Default balance 0 | Set credit_balance &gt; 0 |
| Soft blocks run | Wrong code mapping | Soft must `allowed()==true` |
| Double credit deduct | Aggregate re-run | Uses cost delta, not full cost |
| Invalid JSON ignored | Parser warn log | Fix quota_config shape |

## Related

- Developer TDD: [`../tdd/stories/w5/W5-US04-tdd.md`](../tdd/stories/w5/W5-US04-tdd.md)
- Aggregates: [`W5-US03-hourly-aggregates.md`](W5-US03-hourly-aggregates.md)
- Run block 402: W5-US06
- Architecture §6.2
