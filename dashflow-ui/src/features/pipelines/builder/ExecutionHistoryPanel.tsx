import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import type { PipelineExecutionSummary } from '../../../api/types'
import { isTerminalExecutionStatus } from './executionOverlayReducer'

export const EXECUTION_HISTORY_PAGE_SIZE = 3

type Props = {
  executions: PipelineExecutionSummary[]
  loading?: boolean
  error?: string | null
  selectedId?: string | null
  pipelineId?: string | null
  onSelect: (execution: PipelineExecutionSummary) => void
  compact?: boolean
  /** Hide the panel title (when wrapped in BuilderCollapsible). */
  hideHeader?: boolean
  pageSize?: number
}

function formatWhen(iso?: string | null): string {
  if (!iso) {
    return '—'
  }
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) {
    return iso
  }
  return d.toLocaleString()
}

function formatDuration(
  started?: string | null,
  completed?: string | null,
): string {
  if (!started) {
    return '—'
  }
  const start = new Date(started).getTime()
  const end = completed ? new Date(completed).getTime() : Date.now()
  if (Number.isNaN(start) || Number.isNaN(end) || end < start) {
    return '—'
  }
  const sec = Math.round((end - start) / 1000)
  if (sec < 60) {
    return `${sec}s`
  }
  const min = Math.floor(sec / 60)
  const rem = sec % 60
  return `${min}m ${rem}s`
}

function statusClass(status: string): string {
  const s = status.toUpperCase()
  if (s === 'COMPLETED' || s === 'SUCCEEDED') {
    return 'exec-status completed'
  }
  if (s === 'FAILED') {
    return 'exec-status failed'
  }
  if (s === 'RUNNING' || s === 'PENDING') {
    return 'exec-status running'
  }
  return 'exec-status'
}

export function ExecutionHistoryPanel({
  executions,
  loading,
  error,
  selectedId,
  pipelineId,
  onSelect,
  compact,
  hideHeader,
  pageSize = EXECUTION_HISTORY_PAGE_SIZE,
}: Props) {
  const [page, setPage] = useState(0)

  const totalPages = Math.max(1, Math.ceil(executions.length / pageSize) || 1)

  useEffect(() => {
    if (!selectedId || executions.length === 0) {
      return
    }
    const idx = executions.findIndex((e) => e.id === selectedId)
    if (idx < 0) {
      return
    }
    const target = Math.floor(idx / pageSize)
    setPage(target)
  }, [selectedId, executions, pageSize])

  useEffect(() => {
    setPage((p) => Math.min(p, Math.max(0, totalPages - 1)))
  }, [totalPages])

  const pageRows = useMemo(() => {
    const start = page * pageSize
    return executions.slice(start, start + pageSize)
  }, [executions, page, pageSize])

  const from = executions.length === 0 ? 0 : page * pageSize + 1
  const to = Math.min(executions.length, (page + 1) * pageSize)

  return (
    <section
      className={
        compact ? 'execution-history compact' : 'execution-history'
      }
      aria-label="Execution history"
    >
      {!hideHeader ? (
        <div className="execution-history-header">
          <h2>Run history</h2>
          {pipelineId && !compact ? (
            <Link
              className="muted"
              to={`/observability?pipelineId=${encodeURIComponent(pipelineId)}`}
            >
              Observability →
            </Link>
          ) : null}
        </div>
      ) : pipelineId && !compact ? (
        <div className="execution-history-header">
          <span />
          <Link
            className="muted"
            to={`/observability?pipelineId=${encodeURIComponent(pipelineId)}`}
          >
            Observability →
          </Link>
        </div>
      ) : null}

      {loading ? <p className="muted">Loading runs…</p> : null}
      {error ? (
        <p role="alert" className="obs-warn">
          {error}
        </p>
      ) : null}

      {!loading && !error && executions.length === 0 ? (
        <p className="muted" data-testid="execution-history-empty">
          No runs yet. Deploy and click Run to create the first execution.
        </p>
      ) : null}

      {executions.length > 0 ? (
        <>
          <div className="execution-history-table-wrap">
            <table className="entity-table execution-history-table">
              <thead>
                <tr>
                  <th scope="col">Started</th>
                  <th scope="col">Status</th>
                  <th scope="col">Trigger</th>
                  <th scope="col">Duration</th>
                  <th scope="col">Records</th>
                  <th scope="col">Completeness</th>
                </tr>
              </thead>
              <tbody>
                {pageRows.map((ex) => {
                  const selected = selectedId === ex.id
                  const live = !isTerminalExecutionStatus(ex.status)
                  return (
                    <tr
                      key={ex.id}
                      className={selected ? 'row-active' : undefined}
                      data-testid={`execution-row-${ex.id}`}
                      data-selected={selected ? 'true' : 'false'}
                    >
                      <td>
                        <button
                          type="button"
                          className="linkish"
                          onClick={() => onSelect(ex)}
                        >
                          {formatWhen(ex.started_at)}
                        </button>
                        <div className="muted exec-id" title={ex.id}>
                          {ex.id.slice(0, 8)}…
                          {live ? ' · live' : ''}
                        </div>
                      </td>
                      <td>
                        <span className={statusClass(ex.status)}>
                          {ex.status}
                        </span>
                      </td>
                      <td>{ex.trigger ?? '—'}</td>
                      <td>
                        {formatDuration(ex.started_at, ex.completed_at)}
                      </td>
                      <td>
                        {ex.records_in ?? 0} → {ex.records_out ?? 0}
                      </td>
                      <td>
                        {ex.completeness_pct != null
                          ? `${ex.completeness_pct}%`
                          : '—'}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <div
            className="execution-history-pager"
            role="navigation"
            aria-label="Run history pages"
          >
            <span className="muted" data-testid="execution-history-range">
              {from}–{to} of {executions.length}
            </span>
            <div className="execution-history-pager-actions">
              <button
                type="button"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                aria-label="Previous page"
              >
                Previous
              </button>
              <span data-testid="execution-history-page">
                Page {page + 1} / {totalPages}
              </span>
              <button
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() =>
                  setPage((p) => Math.min(totalPages - 1, p + 1))
                }
                aria-label="Next page"
              >
                Next
              </button>
            </div>
          </div>
        </>
      ) : null}
    </section>
  )
}
