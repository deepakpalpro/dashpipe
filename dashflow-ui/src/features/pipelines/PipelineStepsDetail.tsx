import { useMemo } from 'react'
import type { PipelineStepResponse } from '../../api/types'
import { PIPELET_FIXTURE } from '../pipelets/fixture'
import type { PipeletCatalogEntry } from '../pipelets/catalogFilter'
import { displayConfigValue } from '../../api/secrets'

type Props = {
  steps: PipelineStepResponse[]
  catalog?: PipeletCatalogEntry[]
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

export function PipelineStepsDetail({
  steps,
  catalog = PIPELET_FIXTURE,
}: Props) {
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
        {ordered.map((s) => {
          const meta = byId.get(s.pipelet_id)
          const deploymentEntries = Object.entries(s.deployment_config ?? {})
          const executionEntries = Object.entries(
            s.execution_config ?? s.config ?? {},
          )
          return (
            <li
              key={s.id ?? `${s.step_order}-${s.pipelet_id}`}
              className="step-card"
            >
              <div className="step-card-header">
                <span className="step-order">#{s.step_order}</span>
                <div>
                  <div className="list-item-title">
                    {meta?.name ?? s.pipelet_id}
                  </div>
                  <div className="list-item-meta">
                    {meta?.category ?? 'Unknown'} ·{' '}
                    <code>{s.pipelet_id}</code>
                    {meta?.runtime ? ` · ${meta.runtime}` : ''}
                    {meta?.version ? ` · v${meta.version}` : ''}
                  </div>
                </div>
              </div>

              {meta?.description ? (
                <p className="muted step-desc">{meta.description}</p>
              ) : null}

              <dl className="step-attrs">
                <div>
                  <dt>Connectors</dt>
                  <dd>
                    {s.connector_ids && s.connector_ids.length > 0
                      ? s.connector_ids.join(', ')
                      : '—'}
                  </dd>
                </div>
                <div>
                  <dt>Services</dt>
                  <dd>
                    {s.service_ids && s.service_ids.length > 0
                      ? s.service_ids.join(', ')
                      : '—'}
                  </dd>
                </div>
                <div>
                  <dt>Input queue</dt>
                  <dd>
                    <code>{s.input_queue ?? '—'}</code>
                  </dd>
                </div>
                <div>
                  <dt>Output queue</dt>
                  <dd>
                    <code>{s.output_queue ?? '—'}</code>
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
            </li>
          )
        })}
      </ol>
    </div>
  )
}
