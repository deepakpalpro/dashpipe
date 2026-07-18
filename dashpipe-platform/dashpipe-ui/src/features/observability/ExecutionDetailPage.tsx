import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getExecutionLogs, getPipelineExecution } from '../../api/resources'
import type {
  ExecutionLogEntry,
  ExecutionStepStatusDto,
} from '../../api/types'
import { useTenant } from '../../contexts/TenantContext'
import { isTerminalExecutionStatus } from '../pipelines/builder/executionOverlayReducer'

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
  const ms = end - start
  if (ms < 1000) {
    return `${ms}ms`
  }
  const sec = Math.round(ms / 1000)
  if (sec < 60) {
    return `${sec}s`
  }
  const min = Math.floor(sec / 60)
  const rem = sec % 60
  return `${min}m ${rem}s`
}

function statusClass(status?: string | null): string {
  const s = (status ?? '').toUpperCase()
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

function logLine(entry: ExecutionLogEntry): string {
  const ts = entry['@timestamp'] ? formatWhen(entry['@timestamp']) : ''
  const level = (entry.level ?? 'INFO').toUpperCase()
  const pod = entry.pod_name ? `(${entry.pod_name})` : ''
  const counts =
    entry.records_in != null || entry.records_out != null
      ? ` in=${entry.records_in ?? '—'} out=${entry.records_out ?? '—'}`
      : ''
  const dur = entry.duration_ms != null ? ` ${entry.duration_ms}ms` : ''
  return `${ts} ${level} ${pod} ${entry.message ?? ''}${counts}${dur}`.trim()
}

/** Logs for a stage: prefer pod name (…-stage-N), else fall back to pipelet id. */
function logsForStep(
  logs: ExecutionLogEntry[],
  step: ExecutionStepStatusDto,
): ExecutionLogEntry[] {
  const stageRe = new RegExp(`stage-${step.step_order}(?:\\b|-|$)`)
  const byPod = logs.filter((l) => l.pod_name && stageRe.test(l.pod_name))
  if (byPod.length > 0) {
    return byPod
  }
  if (step.pipelet_id) {
    return logs.filter((l) => l.pipelet_id === step.pipelet_id)
  }
  return []
}

export function ExecutionDetailPage() {
  const { tenantId } = useTenant()
  const { pipelineId = '', executionId = '' } = useParams()
  const [selectedStep, setSelectedStep] = useState<number | null>(null)

  const ns = `tenant-${(tenantId || 'T001').toLowerCase()}`

  const detailQuery = useQuery({
    queryKey: ['pipeline-execution', tenantId, pipelineId, executionId],
    queryFn: () => getPipelineExecution(tenantId, pipelineId, executionId),
    enabled: Boolean(pipelineId && executionId),
    refetchInterval: (query) => {
      const data = query.state.data
      if (!data || isTerminalExecutionStatus(data.status)) {
        return false
      }
      return 2000
    },
  })

  const logsQuery = useQuery({
    queryKey: ['execution-logs', tenantId, executionId],
    queryFn: () => getExecutionLogs(tenantId, executionId),
    enabled: Boolean(executionId),
    staleTime: 0,
    refetchInterval: (query) => {
      const detail = detailQuery.data
      if (detail && isTerminalExecutionStatus(detail.status)) {
        return false
      }
      return query.state.dataUpdateCount < 60 ? 2000 : false
    },
  })

  const execution = detailQuery.data
  const steps = useMemo<ExecutionStepStatusDto[]>(
    () =>
      [...(execution?.steps ?? [])].sort(
        (a, b) => a.step_order - b.step_order,
      ),
    [execution?.steps],
  )
  const logs = useMemo<ExecutionLogEntry[]>(
    () =>
      logsQuery.data?.execution_id === executionId
        ? (logsQuery.data.logs ?? [])
        : [],
    [logsQuery.data, executionId],
  )

  const selected = steps.find((s) => s.step_order === selectedStep) ?? null
  const selectedLogs = selected ? logsForStep(logs, selected) : logs

  const backLink = pipelineId
    ? `/observability?pipelineId=${encodeURIComponent(pipelineId)}`
    : '/observability'

  return (
    <section className="obs-page execution-detail" aria-label="Execution detail">
      <div className="panel-header obs-header">
        <div>
          <Link className="muted execution-detail-back" to={backLink}>
            ← Back to run history
          </Link>
          <h1>Run detail</h1>
          <p className="muted">
            Execution <code title={executionId}>{executionId.slice(0, 8)}…</code>
          </p>
        </div>
      </div>

      {detailQuery.isLoading ? (
        <p className="muted">Loading run…</p>
      ) : detailQuery.isError ? (
        <p role="alert" className="obs-warn">
          Failed to load this run.
        </p>
      ) : !execution ? (
        <p className="muted">Run not found.</p>
      ) : (
        <>
          <dl className="execution-debug-meta" data-testid="execution-detail-meta">
            <div>
              <dt>Status</dt>
              <dd>
                <span className={statusClass(execution.status)}>
                  {execution.status}
                </span>
              </dd>
            </div>
            <div>
              <dt>Started</dt>
              <dd>{formatWhen(execution.started_at)}</dd>
            </div>
            <div>
              <dt>Completed</dt>
              <dd>{formatWhen(execution.completed_at)}</dd>
            </div>
            <div>
              <dt>Duration</dt>
              <dd>
                {formatDuration(execution.started_at, execution.completed_at)}
              </dd>
            </div>
            <div>
              <dt>Records</dt>
              <dd>
                in {execution.records_in ?? 0} / out{' '}
                {execution.records_out ?? 0}
              </dd>
            </div>
            <div>
              <dt>Completeness</dt>
              <dd>
                {execution.completeness_pct != null
                  ? `${execution.completeness_pct}%`
                  : '—'}
              </dd>
            </div>
          </dl>

          {execution.error_summary ? (
            <div className="execution-debug-error" role="alert">
              <h3>Error summary</h3>
              <pre>{execution.error_summary}</pre>
            </div>
          ) : null}

          <section className="obs-panel" aria-label="Pipelet stages">
            <h2>Pipelets</h2>
            {steps.length === 0 ? (
              <p className="muted">
                No per-stage status for this run. Stage tracking requires an API
                restart after the latest update — then start a <strong>new</strong>{' '}
                run. Older executions won&apos;t have pipelet rows.
              </p>
            ) : (
              <div className="execution-history-table-wrap">
                <table className="entity-table execution-detail-steps">
                  <thead>
                    <tr>
                      <th scope="col">Stage</th>
                      <th scope="col">Pipelet</th>
                      <th scope="col">Status</th>
                      <th scope="col">Started</th>
                      <th scope="col">Ended</th>
                      <th scope="col">Duration</th>
                      <th scope="col">Records</th>
                    </tr>
                  </thead>
                  <tbody>
                    {steps.map((s) => {
                      const isSel = selectedStep === s.step_order
                      return (
                        <tr
                          key={s.step_order}
                          className={isSel ? 'row-active' : undefined}
                          data-testid={`stage-row-${s.step_order}`}
                        >
                          <td>{s.step_order}</td>
                          <td>
                            <button
                              type="button"
                              className="linkish"
                              onClick={() =>
                                setSelectedStep(isSel ? null : s.step_order)
                              }
                            >
                              {s.pipelet_id ?? `stage ${s.step_order}`}
                            </button>
                          </td>
                          <td>
                            <span className={statusClass(s.status)}>
                              {s.status}
                            </span>
                          </td>
                          <td>{formatWhen(s.started_at)}</td>
                          <td>{formatWhen(s.completed_at)}</td>
                          <td>
                            {formatDuration(s.started_at, s.completed_at)}
                          </td>
                          <td>
                            {s.records_in ?? 0} → {s.records_out ?? 0}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section
            className="obs-panel execution-detail-logs"
            aria-label="Pipelet logs"
          >
            <div className="execution-debug-logs-header">
              <h2>
                {selected
                  ? `Logs · ${selected.pipelet_id ?? `stage ${selected.step_order}`}`
                  : 'Logs · all stages'}
              </h2>
              <div className="execution-detail-logs-actions">
                {selected ? (
                  <button
                    type="button"
                    className="linkish"
                    onClick={() => setSelectedStep(null)}
                  >
                    Show all
                  </button>
                ) : null}
                <button
                  type="button"
                  className="linkish"
                  onClick={() => void logsQuery.refetch()}
                  disabled={logsQuery.isFetching}
                >
                  Refresh
                </button>
              </div>
            </div>
            {!selected ? (
              <p className="muted">
                Select a pipelet above to filter its logs.
              </p>
            ) : null}
            {logsQuery.isError ? (
              <p role="alert">Failed to load logs</p>
            ) : selectedLogs.length === 0 ? (
              <p className="muted">
                No indexed logs for this stage yet. For live Job output use{' '}
                <code>
                  kubectl logs -n {ns} -l dashpipe.io/execution_id=
                  {executionId.slice(0, 8)}…
                </code>
                .
              </p>
            ) : (
              <pre
                className="execution-debug-log-view"
                data-testid="stage-log-view"
                tabIndex={0}
              >
                {selectedLogs.map((e, i) => (
                  <span
                    key={`${e['@timestamp'] ?? i}-${i}`}
                    className="log-line"
                  >
                    {logLine(e)}
                    {'\n'}
                  </span>
                ))}
              </pre>
            )}
          </section>
        </>
      )}
    </section>
  )
}
