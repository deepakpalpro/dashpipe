type Props = {
  data: {
    pipeline_id: string
    tenant_id: string
    total_errors: number
    by_type: { error_type: string; count: number }[]
  } | null
  loading?: boolean
}

export function ErrorsPanel({ data, loading }: Props) {
  if (loading) {
    return (
      <section aria-label="Critical errors panel" className="obs-panel">
        <h2>Critical Errors</h2>
        <p className="muted">Loading…</p>
      </section>
    )
  }
  if (!data || data.total_errors <= 0) {
    return (
      <section aria-label="Critical errors panel" className="obs-panel">
        <h2>Critical Errors</h2>
        <p className="muted">No critical errors recorded for this pipeline.</p>
      </section>
    )
  }

  return (
    <section aria-label="Critical errors panel" className="obs-panel">
      <h2>Critical Errors</h2>
      <dl className="obs-metrics">
        <div>
          <dt>Total</dt>
          <dd data-testid="errors-total">{data.total_errors}</dd>
        </div>
      </dl>
      <table className="entity-table">
        <thead>
          <tr>
            <th scope="col">Type</th>
            <th scope="col">Count</th>
          </tr>
        </thead>
        <tbody>
          {(data.by_type ?? []).map((row) => (
            <tr key={row.error_type}>
              <td>{row.error_type}</td>
              <td>{row.count}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
