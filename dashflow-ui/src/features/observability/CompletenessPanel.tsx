import type { CompletenessResponse } from './types'

type Props = {
  data: CompletenessResponse | null
  loading?: boolean
}

export function CompletenessPanel({ data, loading }: Props) {
  if (loading) {
    return (
      <section aria-label="Completeness panel" className="obs-panel">
        <h2>Completeness</h2>
        <p className="muted">Loading…</p>
      </section>
    )
  }
  if (!data) {
    return (
      <section aria-label="Completeness panel" className="obs-panel">
        <h2>Completeness</h2>
        <p className="muted">No executions</p>
      </section>
    )
  }

  const pct = Number(data.completeness_pct)
  return (
    <section aria-label="Completeness panel" className="obs-panel">
      <h2>Completeness</h2>
      {pct < 95 ? (
        <p className="obs-warn" role="status">
          Completeness below 95% threshold
        </p>
      ) : null}
      <dl className="obs-metrics">
        <div>
          <dt>Completeness %</dt>
          <dd data-testid="completeness-pct">{pct}%</dd>
        </div>
        <div>
          <dt>Records in</dt>
          <dd>{data.records_in}</dd>
        </div>
        <div>
          <dt>Records out</dt>
          <dd>{data.records_out}</dd>
        </div>
        <div>
          <dt>Execution</dt>
          <dd>{data.execution_id}</dd>
        </div>
      </dl>
    </section>
  )
}
