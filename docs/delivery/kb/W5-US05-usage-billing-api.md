# KB: Usage / billing query APIs (Wave 5)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W5-US05 / W5-US05 |
| **Audience** | Platform engineers / billing / support |
| **Product area** | Metering / Billing APIs |

## Prerequisites

- W5-US03 aggregates; W5-US04 quota/credits
- Header **`X-Tenant-Id`** must equal path `{id}` (else **404**)

## Endpoints (§3.5)

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/api/v1/tenants/{id}/usage?period=current` | UTC calendar-month summary from hourly aggregates |
| `GET` | `/api/v1/tenants/{id}/usage/events?page=0&size=20` | Paginated raw `usage_events` |
| `GET` | `/api/v1/tenants/{id}/quota` | Decision + limits + month usage |
| `GET` | `/api/v1/tenants/{id}/billing/periods` | Stub: current month `open` period |

### Fixture tolerance (wave exit)

| Field | Tolerance |
|-------|-----------|
| Quantity | ± `0.000001` absolute |
| Cost | ± `$0.01` absolute |

IT fixture: 2× `platform.pipeline_runs` + 100× `data.records_processed` → cost **$0.0210** (± $0.01).

## curl examples

```bash
TENANT=<uuid>
HDR=(-H "X-Tenant-Id: $TENANT")

curl -s "${HDR[@]}" "http://localhost:8080/api/v1/tenants/$TENANT/usage?period=current" | jq
curl -s "${HDR[@]}" "http://localhost:8080/api/v1/tenants/$TENANT/usage/events?page=0&size=20" | jq
curl -s "${HDR[@]}" "http://localhost:8080/api/v1/tenants/$TENANT/quota" | jq
curl -s "${HDR[@]}" "http://localhost:8080/api/v1/tenants/$TENANT/billing/periods" | jq
```

Cross-tenant (expect 404):

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "X-Tenant-Id: $OTHER" \
  "http://localhost:8080/api/v1/tenants/$TENANT/usage"
```

## Billing-dispute checklist

1. Compare API `dimensions.*.quantity` to `usage_events` SUM for the UTC month.
2. Confirm hourly `usage_aggregates` cover the disputed hours (`aggregateHour` re-run if needed).
3. Check `credit_balance` vs sum of aggregate `total_cost` deltas.
4. Keep raw events — never delete after aggregate.
5. Soft vs hard: `GET .../quota` → `decision` (`ALLOW` / `SOFT_WARN` / `HARD_BLOCK` / `NO_CREDIT`).

## How to verify

```bash
./mvnw -pl dashflow-api test -Dtest=BillingQueryIT
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| 401 | Missing `X-Tenant-Id` | Send header |
| 404 | Header ≠ path id | Match tenant |
| Empty summary | No aggregates this month | Run aggregate job / seed events |
| Cost mismatch | Stub prices / unaggregated events | See `UsageUnitPrices`; re-aggregate |

## Related

- Developer TDD: [`../tdd/stories/w5/W5-US05-tdd.md`](../tdd/stories/w5/W5-US05-tdd.md)
- Quota: [`W5-US04-quota-credits.md`](W5-US04-quota-credits.md)
- Architecture §3.5
