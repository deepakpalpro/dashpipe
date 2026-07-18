# KB: Run / dry-run / execution overlay (Wave 6)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W6-US05 / W6-US05 |
| **Audience** | Frontend engineers / support / demo |
| **Product area** | UI pipeline run |

## Prerequisites

- W6-US04 builder save
- Optional MSW: `VITE_ENABLE_MSW=true npm run dev`

## Feature overview

Builder bottom bar: **Dry Run** | **Save** | **Run**.

| Action | API | UI |
|--------|-----|-----|
| Dry Run | `POST .../dry-run` (MSW stub OK) | Status line |
| Save | `POST` pipeline + `PUT` steps | Save status |
| Run | `POST .../run` → poll `GET .../executions/{id}` | Overlay colours + summary |
| Quota block | **402** body `code` / `message` | `QuotaBlockedAlert` |

Overlay states: `pending` | `running` (green) | `completed` (blue) | `failed` (red).

### Manual E2E script (interim DoD)

1. `cd dashpipe-ui && VITE_ENABLE_MSW=true npm run dev`
2. Pipelines → add REST Source, JSON Transform, S3 Destination
3. Save → confirm status id
4. Dry Run → “Dry-run OK”
5. Run → overlay progresses to completed for n1–n3
6. (Quota) In tests, `mockDb.blockRunsWith402 = true` → Run shows “no credit” alert

```bash
npm test -- executionOverlayReducer ExecutionOverlay RunControls.quota
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Infinite Running… | Poller maxAttempts / terminal status | MSW completes on 2nd poll |
| Generic error on quota | `ApiError` status 402 | `QuotaBlockedAlert` parses `code` |
| Overlay idle after run | `execution.steps` mapping | Ensure node order matches step_order |

## Related

- Developer TDD: [`../tdd/stories/w6/W6-US05-tdd.md`](../tdd/stories/w6/W6-US05-tdd.md)
- W5-US06 402 · W2 run API · Architecture §4.3–§4.4
