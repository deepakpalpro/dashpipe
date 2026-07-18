# KB: Run blocked with HTTP 402 (Wave 5)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W5-US06 / W5-US06 |
| **Audience** | Platform engineers / billing / support |
| **Product area** | Metering / Quota enforcement |

## Prerequisites

- W5-US04 `QuotaEvaluator` / `QuotaService`
- W2 `POST /api/v1/pipelines/{id}/run`

## Feature overview

Before `orchestrator.start`, `PipelineRunService` calls `QuotaService.evaluateTenant`.

| Decision | HTTP | Execution started? |
|----------|------|--------------------|
| `ALLOW` / `SOFT_WARN` | **202** | Yes |
| `HARD_BLOCK` / `NO_CREDIT` | **402** | **No** |

### 402 body example

```json
{
  "error": "payment_required",
  "code": "NO_CREDIT",
  "message": "credit balance exhausted",
  "credit_balance": 0.0000,
  "breached_dimension": null,
  "soft_limit": null,
  "hard_limit": null,
  "current_usage": null
}
```

Hard block includes `breached_dimension`, `soft_limit`, `hard_limit`, `current_usage`.

### New tenant credit

`TenantService` sets `credit_balance = 100.0000` on create so trial tenants can run. DB column default remains `0` for legacy rows — top up those tenants explicitly.

## Why did my run return 402?

1. `GET /api/v1/tenants/{id}/quota` with matching `X-Tenant-Id`.
2. If `decision=NO_CREDIT` → top up: `UPDATE tenants SET credit_balance = 100 WHERE id = ...` (or `CreditBalanceService.setBalance`).
3. If `decision=HARD_BLOCK` → raise hard limit in `quota_config`, wait for period roll, or reduce usage.
4. Soft warn alone never returns 402.

## How to unblock

```sql
UPDATE tenants SET credit_balance = 100.0000 WHERE id = '<tenant-uuid>';
-- optional: clear or raise hard limits
UPDATE tenants SET quota_config = NULL WHERE id = '<tenant-uuid>';
```

## How to verify

```bash
./mvnw -pl dashpipe-api test -Dtest=RunBlockedIT,PipelineRunServiceQuotaGateTest
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| All runs 402 | New tenants with old binary (credit 0) | Redeploy W5-US06+; or set credit |
| Soft returns 402 | Wrong gate | Soft must allow |
| Execution row on 402 | Gate after start | Gate is before `orchestrator.start` |

## Related

- Developer TDD: [`../tdd/stories/w5/W5-US06-tdd.md`](../tdd/stories/w5/W5-US06-tdd.md)
- Quota: [`W5-US04-quota-credits.md`](W5-US04-quota-credits.md)
- Billing dispute: [`W5-US05-usage-billing-api.md`](W5-US05-usage-billing-api.md)
- Architecture §6.2
