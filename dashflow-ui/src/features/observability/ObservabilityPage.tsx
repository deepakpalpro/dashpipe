import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  getCompleteness,
  getHeartbeat,
  getLatency,
  getObservabilityLinks,
  getPipelineErrors,
  listPipelineExecutions,
  listPipelines,
} from '../../api/resources'
import { useTenant } from '../../contexts/TenantContext'
import { ExecutionHistoryPanel } from '../pipelines/builder/ExecutionHistoryPanel'
import { CompletenessPanel } from './CompletenessPanel'
import { ErrorsPanel } from './ErrorsPanel'
import { HeartbeatPanel } from './HeartbeatPanel'
import { LatencyPanel } from './LatencyPanel'
import { ObservabilityExternalLinks } from './ObservabilityExternalLinks'
import type { TimeRange } from './types'

type Tab = 'runs' | 'completeness' | 'latency' | 'heartbeat' | 'errors'

const TABS: { id: Tab; label: string }[] = [
  { id: 'runs', label: 'Run history' },
  { id: 'completeness', label: 'Completeness' },
  { id: 'latency', label: 'Latency' },
  { id: 'heartbeat', label: 'Heartbeat' },
  { id: 'errors', label: 'Critical Errors' },
]

export function ObservabilityPage() {
  const { tenantId } = useTenant()
  const [searchParams, setSearchParams] = useSearchParams()
  const [tab, setTab] = useState<Tab>('runs')
  const [range, setRange] = useState<TimeRange>('24h')

  const pipelinesQuery = useQuery({
    queryKey: ['pipelines', tenantId],
    queryFn: () => listPipelines(tenantId),
  })

  const pipelines = (pipelinesQuery.data ?? []).filter(
    (p) => p.status.toUpperCase() !== 'ARCHIVED',
  )
  const pipelineIdFromUrl = searchParams.get('pipelineId') ?? ''
  const selectedId = pipelines.some((p) => p.id === pipelineIdFromUrl)
    ? pipelineIdFromUrl
    : (pipelines[0]?.id ?? '')
  const selectedExecutionId = searchParams.get('executionId')

  function selectPipeline(id: string) {
    const next: Record<string, string> = {}
    if (id) {
      next.pipelineId = id
    }
    setSearchParams(next, { replace: true })
  }

  const linksQuery = useQuery({
    queryKey: [
      'obs-links',
      tenantId,
      selectedId,
      selectedExecutionId,
    ],
    queryFn: () =>
      getObservabilityLinks(tenantId, {
        pipelineId: selectedId || undefined,
        executionId: selectedExecutionId || undefined,
      }),
  })

  const executionsQuery = useQuery({
    queryKey: ['pipeline-executions', tenantId, selectedId],
    queryFn: () => listPipelineExecutions(tenantId, selectedId),
    enabled: Boolean(selectedId) && tab === 'runs',
  })

  const completenessQuery = useQuery({
    queryKey: ['obs-completeness', tenantId, selectedId, range],
    queryFn: () => getCompleteness(tenantId, selectedId),
    enabled: Boolean(selectedId) && tab === 'completeness',
  })
  const latencyQuery = useQuery({
    queryKey: ['obs-latency', tenantId, selectedId, range],
    queryFn: () => getLatency(tenantId, selectedId),
    enabled: Boolean(selectedId) && tab === 'latency',
  })
  const heartbeatQuery = useQuery({
    queryKey: ['obs-heartbeat', tenantId, selectedId, range],
    queryFn: () => getHeartbeat(tenantId, selectedId),
    enabled: Boolean(selectedId) && tab === 'heartbeat',
  })
  const errorsQuery = useQuery({
    queryKey: ['obs-errors', tenantId, selectedId, range],
    queryFn: () => getPipelineErrors(tenantId, selectedId),
    enabled: Boolean(selectedId) && tab === 'errors',
  })

  const selector = useMemo(
    () => (
      <div className="obs-controls">
        <label>
          Pipeline
          <select
            aria-label="Pipeline"
            value={selectedId}
            onChange={(e) => selectPipeline(e.target.value)}
          >
            {pipelines.length === 0 ? (
              <option value="">No pipelines</option>
            ) : null}
            {pipelines.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </label>
        {tab !== 'runs' ? (
          <label>
            Time range
            <select
              aria-label="Time range"
              value={range}
              onChange={(e) => setRange(e.target.value as TimeRange)}
            >
              <option value="1h">Last 1 hour</option>
              <option value="24h">Last 24 hours</option>
              <option value="7d">Last 7 days</option>
            </select>
          </label>
        ) : null}
      </div>
    ),
    [pipelines, range, selectedId, tab],
  )

  return (
    <section className="obs-page" aria-label="Observability">
      <div className="panel-header obs-header">
        <h1>Observability</h1>
        <ObservabilityExternalLinks
          links={linksQuery.data}
          loading={linksQuery.isLoading}
        />
      </div>

      <div className="tab-row" role="tablist" aria-label="Observability panels">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={tab === t.id}
            className={tab === t.id ? 'tab active' : 'tab'}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {selector}

      {tab === 'runs' ? (
        <section className="obs-panel" aria-label="Run history panel">
          {selectedId ? (
            <>
              <ExecutionHistoryPanel
                compact
                pipelineId={selectedId}
                executions={executionsQuery.data ?? []}
                loading={executionsQuery.isLoading}
                error={
                  executionsQuery.isError
                    ? executionsQuery.error instanceof Error
                      ? executionsQuery.error.message
                      : 'Failed to load run history'
                    : null
                }
                selectedId={selectedExecutionId}
                onSelect={(ex) => {
                  setSearchParams(
                    {
                      pipelineId: selectedId,
                      executionId: ex.id,
                    },
                    { replace: true },
                  )
                }}
              />
              {selectedExecutionId ? (
                <p className="muted">
                  Selected run{' '}
                  <code>{selectedExecutionId}</code>.{' '}
                  <Link
                    to={`/pipelines/${selectedId}?executionId=${encodeURIComponent(selectedExecutionId)}`}
                  >
                    Open in builder
                  </Link>
                </p>
              ) : null}
            </>
          ) : (
            <p className="muted">Select a pipeline to view run history.</p>
          )}
        </section>
      ) : null}

      {tab === 'completeness' ? (
        <CompletenessPanel
          data={completenessQuery.data ?? null}
          loading={completenessQuery.isLoading}
        />
      ) : null}
      {tab === 'latency' ? (
        <LatencyPanel
          data={latencyQuery.data ?? null}
          loading={latencyQuery.isLoading}
        />
      ) : null}
      {tab === 'heartbeat' ? (
        <HeartbeatPanel
          data={heartbeatQuery.data ?? null}
          loading={heartbeatQuery.isLoading}
        />
      ) : null}
      {tab === 'errors' ? (
        <ErrorsPanel
          data={errorsQuery.data ?? null}
          loading={errorsQuery.isLoading}
        />
      ) : null}
    </section>
  )
}
