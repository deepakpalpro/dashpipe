import { useQuery } from '@tanstack/react-query'
import { getExecutionLogs, getPipelineExecution } from '../../../api/resources'
import type {
  ExecutionLogEntry,
  PipelineExecutionDetail,
  PipelineExecutionSummary,
} from '../../../api/types'
import { useTenant } from '../../../contexts/TenantContext'
import { isTerminalExecutionStatus } from './executionOverlayReducer'

type Props = {
  executionId: string | null
  /** Optional live poller detail — used only when it matches executionId. */
  execution?: PipelineExecutionDetail | null
  /** Optional history row for instant status while detail loads. */
  summary?: PipelineExecutionSummary | null
  pipelineId?: string | null
  /** Hide the panel title (when wrapped in BuilderCollapsible). */
  hideHeader?: boolean
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

function looksLikeStub(execution?: PipelineExecutionDetail | null): boolean {
  if (!execution) {
    return false
  }
  const status = (execution.status ?? '').toUpperCase()
  if (status !== 'COMPLETED' && status !== 'SUCCEEDED') {
    return false
  }
  const inCount = execution.records_in ?? 0
  const outCount = execution.records_out ?? 0
  if (inCount > 0 || outCount > 0) {
    return false
  }
  if (!execution.started_at || !execution.completed_at) {
    return true
  }
  const ms =
    new Date(execution.completed_at).getTime() -
    new Date(execution.started_at).getTime()
  return Number.isFinite(ms) && ms >= 0 && ms < 15_000
}

function logLine(entry: ExecutionLogEntry): string {
  const ts = entry['@timestamp'] ? formatWhen(entry['@timestamp']) : ''
  const level = (entry.level ?? 'INFO').toUpperCase()
  const pipelet = entry.pipelet_id ? `[${entry.pipelet_id}]` : ''
  const pod = entry.pod_name ? `(${entry.pod_name})` : ''
  const counts =
    entry.records_in != null || entry.records_out != null
      ? ` in=${entry.records_in ?? '—'} out=${entry.records_out ?? '—'}`
      : ''
  const dur = entry.duration_ms != null ? ` ${entry.duration_ms}ms` : ''
  return `${ts} ${level} ${pipelet}${pod} ${entry.message ?? ''}${counts}${dur}`.trim()
}

export function ExecutionDebugPanel({
  executionId,
  execution: polledExecution,
  summary,
  pipelineId,
  hideHeader,
}: Props) {
  const { tenantId } = useTenant()
  const ns = `tenant-${(tenantId || 'T001').toLowerCase()}`

  const detailQuery = useQuery({
    queryKey: ['pipeline-execution', tenantId, pipelineId, executionId],
    queryFn: () => getPipelineExecution(tenantId, pipelineId!, executionId!),
    enabled: Boolean(executionId && pipelineId),
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
    queryFn: () => getExecutionLogs(tenantId, executionId!),
    enabled: Boolean(executionId),
    staleTime: 0,
    refetchInterval: (query) => {
      if (!executionId) {
        return false
      }
      const detail = detailQuery.data
      if (detail && isTerminalExecutionStatus(detail.status)) {
        return false
      }
      return query.state.dataUpdateCount < 30 ? 2000 : false
    },
  })

  if (!executionId) {
    return (
      <section className="execution-debug" aria-label="Execution debug">
        {!hideHeader ? (
          <div className="execution-debug-header">
            <h2>Debug / logs</h2>
          </div>
        ) : null}
        <p className="muted">
          Select a run from history (or click Run) to inspect status, indexed
          logs, and kubectl hints.
        </p>
      </section>
    )
  }

  // Never show a previous run's detail under a newly selected id.
  const matchedPoll =
    polledExecution?.id === executionId ? polledExecution : null
  const matchedDetail =
    detailQuery.data?.id === executionId ? detailQuery.data : null
  const execution: PipelineExecutionDetail | PipelineExecutionSummary | null =
    matchedPoll ?? matchedDetail ?? (summary?.id === executionId ? summary : null)

  const logs =
    logsQuery.data?.execution_id === executionId
      ? (logsQuery.data.logs ?? [])
      : []
  const logsLoading =
    logsQuery.isLoading ||
    (logsQuery.isFetching && logsQuery.data?.execution_id !== executionId)

  const stubSmell = looksLikeStub(
    matchedPoll ?? matchedDetail ?? null,
  )
  const kubectlJobs = `kubectl get jobs,pods -n ${ns} -l pipeline.platform/execution_id=${executionId}`
  const kubectlLogs = `kubectl logs -n ${ns} -l pipeline.platform/execution_id=${executionId} --tail=100`
  const petstore = `curl -s http://localhost:4010/api/v3/inventory/summary | python3 -m json.tool`
  const apiExec =
    pipelineId != null
      ? `curl -sf -H "X-Tenant-Id: ${tenantId}" \\\n  "http://localhost:8080/api/v1/pipelines/${pipelineId}/executions/${executionId}" | python3 -m json.tool`
      : null

  return (
    <section
      className="execution-debug"
      aria-label="Execution debug"
      data-execution-id={executionId}
    >
      <div className="execution-debug-header">
        {!hideHeader ? (
          <h2>Debug / logs</h2>
        ) : (
          <span className="muted">Execution</span>
        )}
        <code className="exec-id" title={executionId}>
          {executionId.slice(0, 8)}…
        </code>
      </div>

      {execution ? (
        <dl className="execution-debug-meta" data-testid="execution-debug-meta">
          <div>
            <dt>Status</dt>
            <dd>
              <span
                className={`exec-status ${(execution.status ?? '').toLowerCase()}`}
              >
                {execution.status}
              </span>
            </dd>
          </div>
          <div>
            <dt>Records</dt>
            <dd>
              in {execution.records_in ?? 0} / out {execution.records_out ?? 0}
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
          <div>
            <dt>Started</dt>
            <dd>{formatWhen(execution.started_at)}</dd>
          </div>
          <div>
            <dt>Completed</dt>
            <dd>{formatWhen(execution.completed_at)}</dd>
          </div>
        </dl>
      ) : (
        <p className="muted">Loading execution detail…</p>
      )}

      {stubSmell ? (
        <p className="execution-debug-banner" role="status">
          This run looks like <strong>stub mode</strong> (completed quickly with
          0 records). Real pipelets need API profiles{' '}
          <code>local,k8s</code> — see{' '}
          <code>docs/LOCALDEV_PIPELINE_GUIDE.md</code> or{' '}
          <code>./scripts/localdev.sh start --k8s</code>.
        </p>
      ) : null}

      {matchedDetail?.error_summary || matchedPoll?.error_summary ? (
        <div className="execution-debug-error" role="alert">
          <h3>Error summary</h3>
          <pre>
            {matchedDetail?.error_summary ?? matchedPoll?.error_summary}
          </pre>
        </div>
      ) : null}

      {(matchedDetail?.steps ?? matchedPoll?.steps)?.length ? (
        <div className="execution-debug-steps">
          <h3>Steps</h3>
          <ol>
            {(matchedDetail?.steps ?? matchedPoll?.steps ?? []).map((s) => (
              <li key={s.step_order}>
                <span className={`exec-status ${s.status.toLowerCase()}`}>
                  {s.status}
                </span>{' '}
                stage {s.step_order}
                {s.node_id ? ` · ${s.node_id}` : ''}
              </li>
            ))}
          </ol>
        </div>
      ) : null}

      <div className="execution-debug-logs">
        <div className="execution-debug-logs-header">
          <h3>Indexed logs</h3>
          <button
            type="button"
            className="linkish"
            onClick={() => void logsQuery.refetch()}
            disabled={logsQuery.isFetching}
          >
            Refresh
          </button>
        </div>
        {logsQuery.isError ? (
          <p role="alert">Failed to load logs</p>
        ) : null}
        {logsLoading ? (
          <p className="muted">Loading logs…</p>
        ) : logs.length === 0 ? (
          <p className="muted">
            No indexed logs yet. For live Job output use kubectl (below). Stub
            runs may also emit little or nothing here.
          </p>
        ) : (
          <pre
            className="execution-debug-log-view"
            data-testid="execution-log-view"
            data-execution-id={executionId}
            tabIndex={0}
          >
            {logs.map((e, i) => (
              <span key={`${executionId}-${e['@timestamp'] ?? i}-${i}`} className="log-line">
                {logLine(e)}
                {'\n'}
              </span>
            ))}
          </pre>
        )}
      </div>

      <div className="execution-debug-commands">
        <h3>Dev commands</h3>
        <p className="muted props-hint">
          Namespace <code>{ns}</code>. Guide:{' '}
          <code>docs/LOCALDEV_PIPELINE_GUIDE.md</code>
        </p>
        <pre className="execution-debug-cmd">{kubectlJobs}</pre>
        <pre className="execution-debug-cmd">{kubectlLogs}</pre>
        <pre className="execution-debug-cmd">{petstore}</pre>
        {apiExec ? <pre className="execution-debug-cmd">{apiExec}</pre> : null}
      </div>
    </section>
  )
}
