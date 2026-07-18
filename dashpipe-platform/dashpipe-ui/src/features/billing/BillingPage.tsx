import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import {
  getBillingPeriods,
  getQuotaStatus,
  getUsageEvents,
  getUsageSummary,
} from '../../api/resources'
import { useTenant } from '../../contexts/TenantContext'

function money(value: number | string | null | undefined): string {
  if (value == null || value === '') {
    return '—'
  }
  const n = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(n)) {
    return String(value)
  }
  return `$${n.toFixed(4)}`
}

function qty(value: number | string | null | undefined): string {
  if (value == null || value === '') {
    return '—'
  }
  const n = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(n)) {
    return String(value)
  }
  return n.toLocaleString(undefined, { maximumFractionDigits: 4 })
}

function formatWhen(iso: string | null | undefined): string {
  if (!iso) {
    return '—'
  }
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return iso
  }
}

export function BillingPage() {
  const { tenantId, tenantName } = useTenant()
  const [eventsPage, setEventsPage] = useState(0)

  const usageQuery = useQuery({
    queryKey: ['billing-usage', tenantId],
    queryFn: () => getUsageSummary(tenantId, 'current'),
  })
  const quotaQuery = useQuery({
    queryKey: ['billing-quota', tenantId],
    queryFn: () => getQuotaStatus(tenantId),
  })
  const periodsQuery = useQuery({
    queryKey: ['billing-periods', tenantId],
    queryFn: () => getBillingPeriods(tenantId),
  })
  const eventsQuery = useQuery({
    queryKey: ['billing-events', tenantId, eventsPage],
    queryFn: () => getUsageEvents(tenantId, eventsPage, 20),
  })

  const dimensions = useMemo(() => {
    const map = usageQuery.data?.dimensions ?? {}
    return Object.entries(map).sort(([a], [b]) => a.localeCompare(b))
  }, [usageQuery.data])

  const loading =
    usageQuery.isLoading || quotaQuery.isLoading || periodsQuery.isLoading
  const errored =
    usageQuery.isError || quotaQuery.isError || periodsQuery.isError

  return (
    <section className="billing-page" aria-label="Billing">
      <div className="panel-header">
        <div>
          <h1>Billing</h1>
          <p className="muted billing-subtitle">
            Monthly usage for {tenantName} ({tenantId})
          </p>
        </div>
      </div>

      {loading ? <p className="muted">Loading billing…</p> : null}
      {errored ? (
        <p role="alert">Failed to load billing data for this tenant.</p>
      ) : null}

      <div className="billing-summary-grid">
        <article className="billing-card" aria-label="Credit balance">
          <h2>Credit balance</h2>
          <p className="billing-metric" data-testid="credit-balance">
            {money(
              usageQuery.data?.credit_balance ??
                quotaQuery.data?.credit_balance,
            )}
          </p>
        </article>
        <article className="billing-card" aria-label="Month to date cost">
          <h2>Month-to-date cost</h2>
          <p className="billing-metric" data-testid="mtd-cost">
            {money(usageQuery.data?.total_cost)}
          </p>
          <p className="muted">
            {formatWhen(usageQuery.data?.period_start)} →{' '}
            {formatWhen(usageQuery.data?.period_end)}
          </p>
        </article>
        <article className="billing-card" aria-label="Quota status">
          <h2>Quota</h2>
          <p className="billing-metric" data-testid="quota-decision">
            {quotaQuery.data?.decision ?? '—'}
          </p>
          <p className="muted">{quotaQuery.data?.message ?? ''}</p>
        </article>
      </div>

      <section className="billing-section" aria-label="Usage by dimension">
        <h2>Usage by dimension</h2>
        {dimensions.length === 0 ? (
          <p className="muted">No usage aggregates for the current month yet.</p>
        ) : (
          <table className="entity-table">
            <thead>
              <tr>
                <th>Dimension</th>
                <th>Quantity</th>
                <th>Cost</th>
              </tr>
            </thead>
            <tbody>
              {dimensions.map(([name, dim]) => (
                <tr key={name}>
                  <td>
                    <code>{name}</code>
                  </td>
                  <td>{qty(dim.quantity)}</td>
                  <td>{money(dim.cost)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="billing-section" aria-label="Billing periods">
        <h2>Billing periods</h2>
        {(periodsQuery.data?.periods?.length ?? 0) === 0 ? (
          <p className="muted">No billing periods returned.</p>
        ) : (
          <table className="entity-table">
            <thead>
              <tr>
                <th>Period</th>
                <th>Status</th>
                <th>Total cost</th>
              </tr>
            </thead>
            <tbody>
              {(periodsQuery.data?.periods ?? []).map((p) => (
                <tr key={p.id}>
                  <td>
                    {p.period_start} → {p.period_end}
                  </td>
                  <td>{p.status}</td>
                  <td>{money(p.total_cost)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="billing-section" aria-label="Recent usage events">
        <div className="panel-header">
          <h2>Recent usage events</h2>
          <div className="button-row">
            <button
              type="button"
              className="secondary"
              disabled={eventsPage <= 0 || eventsQuery.isFetching}
              onClick={() => setEventsPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </button>
            <button
              type="button"
              className="secondary"
              disabled={
                eventsQuery.isFetching ||
                (eventsQuery.data != null &&
                  eventsPage + 1 >= eventsQuery.data.total_pages)
              }
              onClick={() => setEventsPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        </div>
        {eventsQuery.isLoading ? <p className="muted">Loading events…</p> : null}
        {eventsQuery.isError ? (
          <p role="alert">Failed to load usage events</p>
        ) : null}
        {(eventsQuery.data?.items?.length ?? 0) === 0 && !eventsQuery.isLoading ? (
          <p className="muted">No usage events yet.</p>
        ) : (
          <table className="entity-table">
            <thead>
              <tr>
                <th>When</th>
                <th>Dimension</th>
                <th>Qty</th>
                <th>Unit</th>
                <th>Pipeline</th>
              </tr>
            </thead>
            <tbody>
              {(eventsQuery.data?.items ?? []).map((e) => (
                <tr key={e.id}>
                  <td>{formatWhen(e.recorded_at)}</td>
                  <td>
                    <code>{e.dimension}</code>
                  </td>
                  <td>{qty(e.quantity)}</td>
                  <td>{e.unit}</td>
                  <td>
                    <code>{e.pipeline_id ?? '—'}</code>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {eventsQuery.data ? (
          <p className="muted">
            Page {eventsQuery.data.page + 1} of{' '}
            {Math.max(1, eventsQuery.data.total_pages)} ·{' '}
            {eventsQuery.data.total_elements} events
          </p>
        ) : null}
      </section>
    </section>
  )
}
