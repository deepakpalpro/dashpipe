import { useId, useMemo, useState } from 'react'
import type { PipelineStepResponse } from '../../api/types'
import type { PipeletCatalogEntry } from '../pipelets/catalogFilter'
import { usePipeletCatalog } from '../pipelets/PipeletCatalogContext'
import { displayConfigValue } from '../../api/secrets'

type Props = {
  steps: PipelineStepResponse[]
  catalog?: PipeletCatalogEntry[]
  /** When true, step bodies start expanded. Default: collapsed. */
  defaultStepsOpen?: boolean
}

function formatValue(value: unknown): string {
  if (value == null) {
    return '—'
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return String(value)
    }
  }
  return String(value)
}

function StepCard({
  step,
  meta,
  defaultOpen,
}: {
  step: PipelineStepResponse
  meta: PipeletCatalogEntry | undefined
  defaultOpen: boolean
}) {
  const panelId = useId()
  const [open, setOpen] = useState(defaultOpen)
  const deploymentEntries = Object.entries(step.deployment_config ?? {})
  const executionEntries = Object.entries(
    step.execution_config ?? step.config ?? {},
  )
  const title = meta?.name ?? step.pipelet_id

  return (
    <li className="step-card" data-open={open ? 'true' : 'false'}>
      <button
        type="button"
        className="step-card-toggle"
        aria-expanded={open}
        aria-controls={panelId}
        aria-label={`Step ${step.step_order}: ${title}`}
        onClick={() => setOpen((current) => !current)}
      >
        <span className="step-card-chevron" aria-hidden="true">
          {open ? '▾' : '▸'}
        </span>
        <span className="step-order">#{step.step_order}</span>
        <span className="step-card-heading">
          <span className="list-item-title">{title}</span>
          <span className="list-item-meta">
            {meta?.category ?? 'Unknown'} · <code>{step.pipelet_id}</code>
            {meta?.runtime ? ` · ${meta.runtime}` : ''}
            {meta?.version ? ` · v${meta.version}` : ''}
          </span>
        </span>
      </button>

      {open ? (
        <div id={panelId} className="step-card-body">
          {meta?.description ? (
            <p className="muted step-desc">{meta.description}</p>
          ) : null}

          <dl className="step-attrs">
            <div>
              <dt>Connectors</dt>
              <dd>
                {step.connector_ids && step.connector_ids.length > 0
                  ? step.connector_ids.join(', ')
                  : '—'}
              </dd>
            </div>
            <div>
              <dt>Services</dt>
              <dd>
                {step.service_ids && step.service_ids.length > 0
                  ? step.service_ids.join(', ')
                  : '—'}
              </dd>
            </div>
            <div>
              <dt>Input queue</dt>
              <dd>
                <code>{step.input_queue ?? '—'}</code>
              </dd>
            </div>
            <div>
              <dt>Output queue</dt>
              <dd>
                <code>{step.output_queue ?? '—'}</code>
              </dd>
            </div>
          </dl>

          <div className="step-config">
            <h4>Deployment configuration</h4>
            {deploymentEntries.length === 0 ? (
              <p className="muted">No deployment keys</p>
            ) : (
              <ul className="deployment-kv">
                {deploymentEntries.map(([k, v]) => (
                  <li key={k}>
                    <code>{k}</code>:{' '}
                    {displayConfigValue(k, v) || formatValue(v)}
                  </li>
                ))}
              </ul>
            )}
            <h4>Execution configuration</h4>
            {executionEntries.length === 0 ? (
              <p className="muted">No execution keys</p>
            ) : (
              <ul className="deployment-kv">
                {executionEntries.map(([k, v]) => (
                  <li key={k}>
                    <code>{k}</code>:{' '}
                    {displayConfigValue(k, v) || formatValue(v)}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      ) : null}
    </li>
  )
}

export function PipelineStepsDetail({
  steps,
  catalog: catalogProp,
  defaultStepsOpen = false,
}: Props) {
  const { catalog: contextCatalog } = usePipeletCatalog()
  const catalog = catalogProp ?? contextCatalog
  const byId = useMemo(() => {
    const map = new Map(catalog.map((c) => [c.id, c]))
    return map
  }, [catalog])

  const ordered = useMemo(
    () => [...steps].sort((a, b) => a.step_order - b.step_order),
    [steps],
  )

  if (ordered.length === 0) {
    return <p className="muted">No steps configured yet.</p>
  }

  return (
    <div className="steps-preview" data-testid="pipeline-steps-detail">
      <h3>Pipelet steps</h3>
      <ol className="step-cards">
        {ordered.map((s) => (
          <StepCard
            key={s.id ?? `${s.step_order}-${s.pipelet_id}`}
            step={s}
            meta={byId.get(s.pipelet_id)}
            defaultOpen={defaultStepsOpen}
          />
        ))}
      </ol>
    </div>
  )
}
