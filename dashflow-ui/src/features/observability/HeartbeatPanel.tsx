import type { HeartbeatResponse } from './types'

type Props = {
  data: HeartbeatResponse | null
  loading?: boolean
}

export function HeartbeatPanel({ data, loading }: Props) {
  if (loading) {
    return (
      <section aria-label="Heartbeat panel" className="obs-panel">
        <h2>Heartbeat</h2>
        <p className="muted">Loading…</p>
      </section>
    )
  }
  if (!data || data.last_heartbeat_epoch_seconds == null) {
    return (
      <section aria-label="Heartbeat panel" className="obs-panel">
        <h2>Heartbeat</h2>
        <p className="muted">
          No heartbeat yet. Heartbeats are recorded when pipelet Jobs complete
          or fail.
        </p>
      </section>
    )
  }

  const lastSeen = new Date(
    data.last_heartbeat_epoch_seconds * 1000,
  ).toISOString()

  return (
    <section aria-label="Heartbeat panel" className="obs-panel">
      <h2>Heartbeat</h2>
      <p data-testid="heartbeat-last-seen">
        Last seen: {lastSeen}
      </p>
      <p data-testid="heartbeat-status">
        Status: {data.stale ? 'Stale' : 'Healthy'}
      </p>
    </section>
  )
}
