import type { LatencyResponse } from './types'

type Props = {
  data: LatencyResponse | null
  loading?: boolean
}

export function LatencyPanel({ data, loading }: Props) {
  if (loading) {
    return (
      <section aria-label="Latency panel" className="obs-panel">
        <h2>Latency</h2>
        <p className="muted">Loading…</p>
      </section>
    )
  }
  if (!data) {
    return (
      <section aria-label="Latency panel" className="obs-panel">
        <h2>Latency</h2>
        <p className="muted">No samples</p>
      </section>
    )
  }
  if (!data.sample_count) {
    return (
      <section aria-label="Latency panel" className="obs-panel">
        <h2>Latency</h2>
        <p className="muted">
          No latency samples yet. Run the pipeline (K8s Jobs emit samples as
          stages complete).
        </p>
      </section>
    )
  }

  return (
    <section aria-label="Latency panel" className="obs-panel">
      <h2>Latency</h2>
      <dl className="obs-metrics">
        <div>
          <dt>Mean</dt>
          <dd>{data.mean_ms} ms</dd>
        </div>
        <div>
          <dt>Max</dt>
          <dd>{data.max_ms} ms</dd>
        </div>
        <div>
          <dt>p50</dt>
          <dd>{data.p50_ms ?? '—'} ms</dd>
        </div>
        <div>
          <dt>p95</dt>
          <dd data-testid="latency-p95">{data.p95_ms ?? data.max_ms} ms</dd>
        </div>
        <div>
          <dt>Samples</dt>
          <dd>{data.sample_count}</dd>
        </div>
      </dl>
    </section>
  )
}
