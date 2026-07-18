# KB: Pipeline builder save (Wave 6)

| Field | Value |
|-------|--------|
| **Article / Story** | KB-W6-US04 / W6-US04 |
| **Audience** | Frontend engineers / support |
| **Product area** | UI pipeline builder |
| **Last updated** | 2026-07-10 |

## Prerequisites

- W6-US01–US03 (`dashflow-ui`)
- Optional: `npm run dev` (MSW) or `npm run dev:api` (live API + Flyway ≥ V17)

## Feature overview

Routes:

| Path | Role |
|------|------|
| `/pipelines` | List + detail (steps, dual configs); archive; open builder |
| `/pipelines/new` | Create builder |
| `/pipelines/:pipelineId` | Edit existing graph |

Builder is three panels (palette | React Flow canvas | properties):

| Piece | Role |
|-------|------|
| `pipelineGraphReducer` | Pure ADD_NODE / CONNECT / UPDATE_STEP / REMOVE / dual config setters |
| `graphToStepsPayload` | Topological order → W2 `PUT .../steps` body with `deployment_config` + `execution_config` |
| `PipeletPalette` | Grouped Source/Processor/Destination; search; show-more (5 preview) |
| `StepPropertiesPanel` | **Searchable** connector/service dropdowns; dual KeyValue editors; remove step |
| Save | `POST /api/v1/pipelines` then `PUT /api/v1/pipelines/{id}/steps` (also sends pipeline dual configs) |
| Dry-run | `POST /api/v1/pipelines/{id}/dry-run` (API + MSW) |

### Dual configuration

1. **Pipeline-level** editors above the canvas: deployment + execution.
2. **Step-level** editors in Properties: seeded from pipelet `deploymentConfiguration` / `executionConfiguration`, then override/extend.
3. Legacy `steps[].config` remains an alias of `execution_config`.

See [`W6-dual-deployment-execution-config.md`](W6-dual-deployment-execution-config.md).

### Searchable connector binding

With ~100 seeded connectors, the Properties **Connector** (and **Service**) control is a searchable select: open → type to filter → pick. Meta shows type id and status.

### Local save without backend

`npm run dev` enables MSW by default. Use `npm run dev:api` (or `VITE_ENABLE_MSW=false`) to call a live API (Vite proxies `/api` → `:8080`).

### `threeStage` fixture shape

Save payload steps (ordered):

1. `plet-rest-source`
2. `plet-json-transform`
3. `plet-s3-destination`

Each step includes `pipelet_id`, `step_order`, `config`, `deployment_config`, `execution_config`, `connector_ids`, `service_ids`, `input_queue`, `output_queue`.

### Remove a step

1. Select the step on the canvas.
2. In **Properties**, click **Remove step** — or focus the canvas and press **Delete** / **Backspace**.

### Manual smoke

1. Open **Pipelines** → New pipeline (or edit an existing one).
2. Add REST Source → JSON Transform → S3 Destination from palette.
3. Select source; search/pick connector. Select destination; pick service.
4. Adjust pipeline or step deployment/execution keys.
5. Save → status shows pipeline id/version; list page shows dual configs + step detail.
6. Dry-run / Run as needed (402 surfaces when credit is zero).

## How to verify

```bash
cd dashflow-ui
npm test -- pipelineGraphReducer PipelineBuilder.save StepPropertiesPanel PipeletPalette PipelinesListPage
```

## Failure modes

| Symptom | Check | Mitigation |
|---------|-------|------------|
| Save 404 on steps | Pipeline create failed | Ensure `X-Tenant-Id`; MSW pipeline handlers present |
| Wrong step order | Edges missing | Palette auto-connect; or connect on canvas |
| Save 404 / failed | MSW off + no API | `npm run dev` or run `dashflow-api` with `npm run dev:api` |
| Connector list huge / hard to find | Plain `<select>` | Use searchable dropdown (current UI) |
| React Flow blank in jsdom | ResizeObserver | Polyfill in `test/setup.ts`; mock canvas in save test |

## Related

- Developer TDD: [`../tdd/stories/w6/W6-US04-tdd.md`](../tdd/stories/w6/W6-US04-tdd.md)
- Dual config: [`W6-dual-deployment-execution-config.md`](W6-dual-deployment-execution-config.md)
- Architecture §4.3–§4.4 · W2 steps API
