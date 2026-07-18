import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useReducer, useState } from 'react'
import {
  Link,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom'
import {
  cancelPipelineExecution,
  createPipeline,
  dryRunPipeline,
  getPipeline,
  listConnectors,
  listPipelineExecutions,
  listServices,
  replacePipelineSteps,
  runPipeline,
  updatePipeline,
} from '../../../api/resources'
import {
  ApiError,
  type PipelineExecutionSummary,
  type QuotaBlockedBody,
} from '../../../api/types'
import { useTenant } from '../../../contexts/TenantContext'
import { KeyValueEditor } from '../../forms/KeyValueEditor'
import { mergeExtendConfig } from '../../forms/mergeExtendConfig'
import { usePipeletCatalog } from '../../pipelets/PipeletCatalogContext'
import type { PipeletCatalogEntry } from '../../pipelets/catalogFilter'
import { ExecutionDebugPanel } from './ExecutionDebugPanel'
import { BuilderCollapsible } from './BuilderCollapsible'
import { ExecutionHistoryPanel } from './ExecutionHistoryPanel'
import { ExecutionOverlaySummary } from './ExecutionOverlaySummary'
import { PipeletPalette } from './PipeletPalette'
import { PipelineCanvas } from './PipelineCanvas'
import { QuotaBlockedAlert } from './QuotaBlockedAlert'
import { RunControls } from './RunControls'
import { StepPropertiesPanel } from './StepPropertiesPanel'
import { PipelineImportExportControls } from '../PipelineImportExportControls'
import {
  executionOverlayReducer,
  initialOverlayState,
  isTerminalExecutionStatus,
} from './executionOverlayReducer'
import {
  graphToStepsPayload,
  initialPipelineGraph,
  orderedNodeIds,
  pipelineGraphReducer,
  stepsToGraph,
  type StepCategory,
} from './pipelineGraphReducer'
import { useExecutionPoller } from './useExecutionPoller'

type Props = {
  catalog?: PipeletCatalogEntry[]
}

/** Keep pipeline scheduleCron in sync with Schedule Source step cron for the platform poller. */
function withScheduleCronFromSteps(
  executionConfig: Record<string, unknown>,
  nodes: Array<{ data: { pipeletId: string; executionConfig?: Record<string, unknown> } }>,
): Record<string, unknown> {
  const scheduleNode = nodes.find(
    (n) => n.data.pipeletId === 'plet-schedule-source',
  )
  const stepCron = scheduleNode?.data.executionConfig?.cron
  if (typeof stepCron === 'string' && stepCron.trim()) {
    return { ...executionConfig, scheduleCron: stepCron.trim() }
  }
  return executionConfig
}

export function PipelineBuilderPage({ catalog: catalogProp }: Props) {
  const { pipelineId: routePipelineId } = useParams<{ pipelineId?: string }>()
  const isNew = !routePipelineId || routePipelineId === 'new'
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const { tenantId } = useTenant()
  const { catalog: contextCatalog } = usePipeletCatalog()
  const catalog = catalogProp ?? contextCatalog
  const queryClient = useQueryClient()
  const [state, dispatch] = useReducer(pipelineGraphReducer, initialPipelineGraph)
  const [overlay, overlayDispatch] = useReducer(
    executionOverlayReducer,
    initialOverlayState,
  )
  const [saveMessage, setSaveMessage] = useState<string | null>(() => {
    const flash = (location.state as { saveMessage?: string } | null)?.saveMessage
    return flash ?? null
  })
  const [dryRunMessage, setDryRunMessage] = useState<string | null>(null)
  const [pipelineId, setPipelineId] = useState<string | null>(
    isNew ? null : routePipelineId,
  )
  const [executionId, setExecutionId] = useState<string | null>(
    () => searchParams.get('executionId'),
  )
  const [quotaInfo, setQuotaInfo] = useState<{
    code: string
    message: string
  } | null>(null)
  const [nodeSeq, setNodeSeq] = useState(1)
  const [running, setRunning] = useState(false)
  const [stopping, setStopping] = useState(false)
  const [pollKey, setPollKey] = useState(0)
  const [deploying, setDeploying] = useState(false)
  const [deactivating, setDeactivating] = useState(false)
  const [pipelineStatus, setPipelineStatus] = useState<string>('DRAFT')
  const [hydratedId, setHydratedId] = useState<string | null>(null)
  const [debugOpen, setDebugOpen] = useState(() => Boolean(searchParams.get('executionId')))
  const [triggerPayloadText, setTriggerPayloadText] = useState('')
  const hasManualSource = state.nodes.some(
    (n) => n.data.pipeletId === 'plet-manual-source',
  )

  function selectExecution(id: string | null, opts?: { resume?: boolean }) {
    setExecutionId(id)
    const next = new URLSearchParams(searchParams)
    if (id) {
      next.set('executionId', id)
    } else {
      next.delete('executionId')
    }
    setSearchParams(next, { replace: true })
    if (opts?.resume) {
      setRunning(true)
    } else if (id === null) {
      setRunning(false)
    }
  }

  const catalogById = useMemo(() => {
    const map = new Map(catalog.map((c) => [c.id, c]))
    return map
  }, [catalog])

  useEffect(() => {
    const flash = (location.state as { saveMessage?: string } | null)?.saveMessage
    if (flash) {
      setSaveMessage(flash)
      navigate(location.pathname, { replace: true, state: null })
    }
  }, [location.state, location.pathname, navigate])

  const pipelineQuery = useQuery({
    queryKey: ['pipeline', tenantId, routePipelineId],
    queryFn: () => getPipeline(tenantId, routePipelineId!),
    enabled: !isNew && Boolean(routePipelineId),
  })

  useEffect(() => {
    if (isNew) {
      if (hydratedId !== null) {
        dispatch({ type: 'RESET' })
        setPipelineId(null)
        setHydratedId(null)
        setNodeSeq(1)
        setPipelineStatus('DRAFT')
        setSaveMessage(null)
        setExecutionId(null)
        overlayDispatch({ type: 'RESET' })
      }
      return
    }
    const pipeline = pipelineQuery.data
    if (!pipeline || hydratedId === pipeline.id) {
      return
    }
    const graph = stepsToGraph(
      pipeline.name,
      pipeline.steps ?? [],
      (pipeletId) => {
        const entry = catalogById.get(pipeletId)
        return {
          name: entry?.name ?? pipeletId,
          category: (entry?.category as StepCategory) ?? 'Processor',
          deploymentConfiguration: entry?.deploymentConfiguration,
          executionConfiguration: entry?.executionConfiguration,
        }
      },
      pipeline.deployment_config ?? {},
      pipeline.execution_config ?? {},
    )
    dispatch({ type: 'RESET', state: graph })
    setPipelineId(pipeline.id)
    setPipelineStatus(pipeline.status ?? 'DRAFT')
    setNodeSeq((pipeline.steps?.length ?? 0) + 1)
    setHydratedId(pipeline.id)
    overlayDispatch({ type: 'RESET' })
  }, [isNew, pipelineQuery.data, hydratedId, catalogById])

  const connectorsQuery = useQuery({
    queryKey: ['connectors', tenantId],
    queryFn: () => listConnectors(tenantId),
  })
  const servicesQuery = useQuery({
    queryKey: ['services', tenantId],
    queryFn: () => listServices(tenantId),
  })

  const executionsQuery = useQuery({
    queryKey: ['pipeline-executions', tenantId, pipelineId],
    queryFn: () => listPipelineExecutions(tenantId, pipelineId!),
    enabled: Boolean(pipelineId),
    refetchInterval: (query) => {
      const rows = query.state.data
      if (!rows?.length) {
        return false
      }
      return rows.some((r) => !isTerminalExecutionStatus(r.status))
        ? 2000
        : false
    },
  })

  useEffect(() => {
    if (!pipelineId || !executionsQuery.isSuccess) {
      return
    }
    const rows = executionsQuery.data ?? []
    const fromUrl = searchParams.get('executionId')

    if (fromUrl) {
      const row = rows.find((r) => r.id === fromUrl)
      if (executionId !== fromUrl) {
        selectExecution(fromUrl, {
          resume: row ? !isTerminalExecutionStatus(row.status) : true,
        })
      } else if (row) {
        setRunning(!isTerminalExecutionStatus(row.status))
      }
      return
    }

    if (executionId || rows.length === 0) {
      return
    }

    const pick =
      rows.find((r) => !isTerminalExecutionStatus(r.status)) ?? rows[0]
    if (pick) {
      selectExecution(pick.id, {
        resume: !isTerminalExecutionStatus(pick.status),
      })
    }
    // selectExecution updates URL; only auto-pick when none selected.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    pipelineId,
    executionsQuery.isSuccess,
    executionsQuery.data,
    searchParams,
    executionId,
  ])

  const selected = useMemo(
    () => state.nodes.find((n) => n.id === state.selectedNodeId) ?? null,
    [state.nodes, state.selectedNodeId],
  )

  const nodeIdsInOrder = useMemo(() => orderedNodeIds(state), [state])

  const { execution } = useExecutionPoller({
    tenantId,
    pipelineId,
    executionId,
    enabled: Boolean(executionId),
    intervalMs: 500,
    maxAttempts: 240,
    pollKey,
  })

  useEffect(() => {
    if (!execution) {
      return
    }
    if (execution.steps?.length) {
      overlayDispatch({
        type: 'APPLY_STEPS',
        nodeIdsInOrder,
        steps: execution.steps,
        executionStatus: execution.status,
      })
    } else {
      overlayDispatch({
        type: 'APPLY_OVERALL',
        nodeIdsInOrder,
        executionStatus: execution.status,
      })
    }
    if (isTerminalExecutionStatus(execution.status)) {
      setRunning(false)
      void queryClient.invalidateQueries({
        queryKey: ['pipeline-executions', tenantId, pipelineId],
      })
    }
  }, [execution, nodeIdsInOrder, queryClient, tenantId, pipelineId])

  const saveMutation = useMutation({
    mutationFn: async () => {
      const executionConfig = withScheduleCronFromSteps(
        state.executionConfig,
        state.nodes,
      )
      if (pipelineId) {
        await updatePipeline(tenantId, pipelineId, {
          name: state.pipelineName,
          deployment_config: state.deploymentConfig,
          execution_config: executionConfig,
        })
        const stepsBody = graphToStepsPayload(state)
        return replacePipelineSteps(tenantId, pipelineId, stepsBody)
      }
      const created = await createPipeline(tenantId, {
        name: state.pipelineName,
        executionMode: 'ASYNC',
        visibility: 'PRIVATE',
        deployment_config: state.deploymentConfig,
        execution_config: executionConfig,
      })
      const stepsBody = graphToStepsPayload(state)
      return replacePipelineSteps(tenantId, created.id, stepsBody)
    },
    onSuccess: (pipeline) => {
      const msg = `Saved ${pipeline.name} (${pipeline.id}) v${pipeline.version}`
      setPipelineId(pipeline.id)
      setHydratedId(pipeline.id)
      setPipelineStatus(pipeline.status ?? 'DRAFT')
      setSaveMessage(msg)
      void queryClient.invalidateQueries({ queryKey: ['pipelines', tenantId] })
      void queryClient.invalidateQueries({
        queryKey: ['pipeline', tenantId, pipeline.id],
      })
      if (isNew || routePipelineId !== pipeline.id) {
        navigate(`/pipelines/${pipeline.id}`, {
          replace: true,
          state: { saveMessage: msg },
        })
      }
    },
    onError: (err: Error) => {
      setSaveMessage(`Save failed: ${err.message}`)
    },
  })

  async function ensureSaved(): Promise<string> {
    if (pipelineId) {
      return pipelineId
    }
    const pipeline = await saveMutation.mutateAsync()
    return pipeline.id
  }

  async function handleDryRun() {
    setQuotaInfo(null)
    try {
      const id = await ensureSaved()
      const result = await dryRunPipeline(tenantId, id)
      setDryRunMessage(
        result.valid
          ? result.messages.join('; ') || 'Dry-run OK'
          : `Invalid: ${result.messages.join('; ')}`,
      )
    } catch (err) {
      setDryRunMessage(err instanceof Error ? err.message : 'Dry-run failed')
    }
  }

  async function handleActivate() {
    setQuotaInfo(null)
    setDryRunMessage(null)
    setDeploying(true)
    try {
      const executionConfig = withScheduleCronFromSteps(
        state.executionConfig,
        state.nodes,
      )
      const id = await ensureSaved()
      const updated = await updatePipeline(tenantId, id, {
        name: state.pipelineName,
        status: 'ACTIVE',
        deployment_config: state.deploymentConfig,
        execution_config: executionConfig,
      })
      setPipelineId(id)
      setPipelineStatus(updated.status ?? 'ACTIVE')
      setSaveMessage(`Activated ${updated.name} (${updated.id})`)
      void queryClient.invalidateQueries({ queryKey: ['pipelines', tenantId] })
      void queryClient.invalidateQueries({
        queryKey: ['pipeline', tenantId, id],
      })
      if (isNew || routePipelineId !== id) {
        navigate(`/pipelines/${id}`, {
          replace: true,
          state: { saveMessage: `Activated ${updated.name}` },
        })
      }
    } catch (err) {
      setSaveMessage(err instanceof Error ? err.message : 'Activate failed')
    } finally {
      setDeploying(false)
    }
  }

  async function handleDeactivate() {
    setQuotaInfo(null)
    setDryRunMessage(null)
    setDeactivating(true)
    try {
      const id = await ensureSaved()
      const updated = await updatePipeline(tenantId, id, {
        name: state.pipelineName,
        status: 'DRAFT',
        deployment_config: state.deploymentConfig,
        execution_config: withScheduleCronFromSteps(
          state.executionConfig,
          state.nodes,
        ),
      })
      setPipelineId(id)
      setPipelineStatus(updated.status ?? 'DRAFT')
      setSaveMessage(`Deactivated ${updated.name} (${updated.id}) — status DRAFT`)
      void queryClient.invalidateQueries({ queryKey: ['pipelines', tenantId] })
      void queryClient.invalidateQueries({
        queryKey: ['pipeline', tenantId, id],
      })
    } catch (err) {
      setSaveMessage(err instanceof Error ? err.message : 'Deactivate failed')
    } finally {
      setDeactivating(false)
    }
  }

  async function handleRun() {
    setQuotaInfo(null)
    setDryRunMessage(null)
    overlayDispatch({ type: 'RESET' })
    setRunning(true)
    setDebugOpen(true)
    try {
      const id = await ensureSaved()
      let payload: unknown
      const raw = triggerPayloadText.trim()
      if (raw) {
        try {
          payload = JSON.parse(raw) as unknown
        } catch {
          setRunning(false)
          setSaveMessage('Trigger payload must be valid JSON')
          return
        }
      }
      const run = await runPipeline(tenantId, id, payload)
      setPipelineId(id)
      selectExecution(run.execution_id, { resume: true })
      void queryClient.invalidateQueries({
        queryKey: ['pipeline-executions', tenantId, id],
      })
      if (isNew || routePipelineId !== id) {
        navigate(
          `/pipelines/${id}?executionId=${encodeURIComponent(run.execution_id)}`,
          { replace: true },
        )
      }
    } catch (err) {
      setRunning(false)
      if (err instanceof ApiError && err.status === 402) {
        const body = err.body as QuotaBlockedBody
        setQuotaInfo({
          code: body?.code ?? 'HARD_BLOCK',
          message:
            body?.message ??
            'Pipeline run blocked by quota or credit limits.',
        })
        return
      }
      setSaveMessage(err instanceof Error ? err.message : 'Run failed')
    }
  }

  async function handleForceStop() {
    if (!pipelineId || !executionId) {
      return
    }
    const stoppedId = executionId
    setQuotaInfo(null)
    setDryRunMessage(null)
    setStopping(true)
    try {
      await cancelPipelineExecution(tenantId, pipelineId, stoppedId)
      setRunning(false)
      setPollKey((k) => k + 1)
      setSaveMessage(
        'Execution force-stopped — Jobs deleted, queues purged. Run fresh when ready.',
      )
      queryClient.setQueryData(
        ['pipeline-executions', tenantId, pipelineId],
        (prev: PipelineExecutionSummary[] | undefined) =>
          (prev ?? []).map((row) =>
            row.id === stoppedId
              ? {
                  ...row,
                  status: 'cancelled',
                  completed_at: new Date().toISOString(),
                }
              : row,
          ),
      )
      void queryClient.invalidateQueries({
        queryKey: ['pipeline-executions', tenantId, pipelineId],
      })
    } catch (err) {
      const detail =
        err instanceof ApiError
          ? err.status === 404
            ? 'Cancel API not found — restart dashpipe-api so Force Stop is available.'
            : err.message
          : err instanceof Error
            ? err.message
            : 'Force stop failed'
      setSaveMessage(detail)
    } finally {
      setStopping(false)
    }
  }

  function handleSelectExecution(ex: PipelineExecutionSummary) {
    overlayDispatch({ type: 'RESET' })
    setDebugOpen(true)
    selectExecution(ex.id, {
      resume: !isTerminalExecutionStatus(ex.status),
    })
  }

  function addFromPalette(item: PipeletCatalogEntry) {
    const id = `n${nodeSeq}`
    setNodeSeq((n) => n + 1)
    const deploymentConfig = mergeExtendConfig(
      item.deploymentConfiguration,
      {},
    )
    const executionConfig = mergeExtendConfig(item.executionConfiguration, {})
    dispatch({
      type: 'ADD_NODE',
      node: {
        id,
        position: { x: 40 + (nodeSeq - 1) * 220, y: 120 },
        data: {
          pipeletId: item.id,
          name: item.name,
          category: item.category,
          connectorIds: [],
          serviceIds: [],
          config: executionConfig,
          deploymentConfig,
          executionConfig,
        },
      },
    })
    if (state.nodes.length > 0) {
      const prev = state.nodes[state.nodes.length - 1]
      dispatch({
        type: 'CONNECT',
        source: prev.id,
        target: id,
        label: `q-stage-${state.nodes.length}`,
      })
    }
  }

  if (!isNew && pipelineQuery.isLoading && hydratedId === null) {
    return (
      <section className="builder-page" aria-label="Pipeline builder">
        <p className="muted">Loading pipeline…</p>
        {saveMessage ? (
          <p role="status" data-testid="save-status">
            {saveMessage}
          </p>
        ) : null}
      </section>
    )
  }

  if (!isNew && pipelineQuery.isError) {
    return (
      <section className="builder-page" aria-label="Pipeline builder">
        <p role="alert">Failed to load pipeline</p>
        <Link to="/pipelines">Back to list</Link>
      </section>
    )
  }

  return (
    <section className="builder-page" aria-label="Pipeline builder">
      <div className="panel-header">
        <div className="builder-title-row">
          <Link className="back-link" to="/pipelines">
            ← Pipelines
          </Link>
          <h1>{isNew ? 'New pipeline' : 'Edit pipeline'}</h1>
          <label className="inline-field">
            <span className="sr-only">Pipeline name</span>
            <input
              aria-label="Pipeline name"
              value={state.pipelineName}
              onChange={(e) =>
                dispatch({ type: 'SET_PIPELINE_NAME', name: e.target.value })
              }
            />
          </label>
        </div>
        <div className="builder-toolbar-actions">
          <RunControls
            canRun={state.nodes.length > 0}
            saving={saveMutation.isPending}
            activating={deploying}
            deactivating={deactivating}
            running={running}
            stopping={stopping}
            canForceStop={Boolean(
              pipelineId &&
                executionId &&
                (running ||
                  (execution &&
                    !isTerminalExecutionStatus(execution.status))),
            )}
            status={pipelineStatus}
            showTriggerPayload={hasManualSource}
            triggerPayloadText={triggerPayloadText}
            onTriggerPayloadChange={setTriggerPayloadText}
            onDryRun={() => {
              void handleDryRun()
            }}
            onSave={() => saveMutation.mutate()}
            onActivate={() => {
              void handleActivate()
            }}
            onDeactivate={() => {
              void handleDeactivate()
            }}
            onRun={() => {
              void handleRun()
            }}
            onForceStop={() => {
              void handleForceStop()
            }}
          />
          {pipelineId ? (
            <PipelineImportExportControls
              tenantId={tenantId}
              pipelineId={pipelineId}
              pipelineName={state.pipelineName}
              onImported={(id, msg) => {
                setSaveMessage(msg)
                void queryClient.invalidateQueries({
                  queryKey: ['pipelines', tenantId],
                })
                navigate(`/pipelines/${id}`)
              }}
              onError={(msg) => setSaveMessage(msg)}
            />
          ) : null}
        </div>
      </div>

      <QuotaBlockedAlert
        info={quotaInfo}
        onDismiss={() => setQuotaInfo(null)}
      />

      {saveMessage ? (
        <p role="status" data-testid="save-status">
          {saveMessage}
        </p>
      ) : null}
      {dryRunMessage ? (
        <p role="status" data-testid="dry-run-status">
          {dryRunMessage}
        </p>
      ) : null}

      <BuilderCollapsible
        title="Data plane & pipeline config"
        summary={
          String(state.executionConfig.ioMode ?? 'queue').toLowerCase() ===
          'stdio'
            ? 'Stdio'
            : 'Queue'
        }
        defaultOpen={false}
        className="builder-deployment"
        aria-label="Pipeline configuration"
      >
        <div className="builder-io-mode" aria-label="Data plane">
          <h3>Data plane</h3>
          <div
            className="status-slider"
            role="radiogroup"
            aria-label="Pipelet I/O mode"
          >
            {(
              [
                { value: 'queue', label: 'Queue' },
                { value: 'stdio', label: 'Stdio' },
              ] as const
            ).map((opt) => {
              const current =
                String(state.executionConfig.ioMode ?? 'queue').toLowerCase() ===
                'stdio'
                  ? 'stdio'
                  : 'queue'
              return (
                <label
                  key={opt.value}
                  className={
                    current === opt.value
                      ? 'status-slider-option selected'
                      : 'status-slider-option'
                  }
                >
                  <input
                    type="radio"
                    name="pipeline-io-mode"
                    value={opt.value}
                    checked={current === opt.value}
                    onChange={() =>
                      dispatch({
                        type: 'SET_EXECUTION_CONFIG',
                        executionConfig: {
                          ...state.executionConfig,
                          ioMode: opt.value,
                        },
                      })
                    }
                  />
                  <span>{opt.label}</span>
                </label>
              )
            })}
          </div>
          <p className="muted props-hint">
            Queue = RabbitMQ between steps (platform). Stdio = local pipe
            chaining.
          </p>
        </div>
        <KeyValueEditor
          title="Deployment configuration"
          entries={state.deploymentConfig}
          onChange={(deploymentConfig) =>
            dispatch({ type: 'SET_DEPLOYMENT_CONFIG', deploymentConfig })
          }
        />
        <KeyValueEditor
          title="Execution configuration"
          entries={state.executionConfig}
          onChange={(executionConfig) =>
            dispatch({ type: 'SET_EXECUTION_CONFIG', executionConfig })
          }
        />
        <p className="muted props-hint">
          Pipeline-level defaults. Steps inherit pipelet defaults and can
          override or extend both maps. Examples: cloud, region, accessKey /
          batchSize, parallelism. Data plane is also stored as{' '}
          <code>execution_config.ioMode</code>.
        </p>
      </BuilderCollapsible>

      <ExecutionOverlaySummary
        byNodeId={overlay.byNodeId}
        nodeIds={nodeIdsInOrder}
      />

      {pipelineId ? (
        <BuilderCollapsible
          title="Run history"
          summary={
            executionsQuery.data?.length
              ? `${executionsQuery.data.length} run${executionsQuery.data.length === 1 ? '' : 's'}`
              : undefined
          }
          defaultOpen
          className="builder-history-card"
        >
          <ExecutionHistoryPanel
            pipelineId={pipelineId}
            executions={executionsQuery.data ?? []}
            loading={executionsQuery.isLoading}
            hideHeader
            error={
              executionsQuery.isError
                ? executionsQuery.error instanceof Error
                  ? executionsQuery.error.message
                  : 'Failed to load run history'
                : null
            }
            selectedId={executionId}
            onSelect={handleSelectExecution}
          />
        </BuilderCollapsible>
      ) : null}

      <BuilderCollapsible
        title="Debug / logs"
        summary={executionId ? executionId.slice(0, 8) + '…' : 'No run selected'}
        open={debugOpen}
        onOpenChange={setDebugOpen}
        className="builder-debug-card"
      >
        <ExecutionDebugPanel
          key={executionId ?? 'none'}
          executionId={executionId}
          execution={execution}
          summary={
            (executionsQuery.data ?? []).find((e) => e.id === executionId) ??
            null
          }
          pipelineId={pipelineId}
          hideHeader
        />
      </BuilderCollapsible>

      <BuilderCollapsible
        title="Pipeline builder"
        summary={`${state.nodes.length} step${state.nodes.length === 1 ? '' : 's'}`}
        defaultOpen
        className="builder-canvas-card"
        aria-label="Pipeline builder"
      >
        <div className="builder-layout">
          <PipeletPalette items={catalog} onAdd={addFromPalette} />
          <PipelineCanvas
            nodes={state.nodes}
            edges={state.edges}
            overlayByNodeId={overlay.byNodeId}
            selectedNodeId={state.selectedNodeId}
            onSelect={(nodeId) => dispatch({ type: 'SELECT_NODE', nodeId })}
            onConnect={(source, target) =>
              dispatch({ type: 'CONNECT', source, target })
            }
            onRemove={(nodeId) => dispatch({ type: 'REMOVE_NODE', nodeId })}
          />
          <StepPropertiesPanel
            node={selected}
            catalog={catalog}
            connectors={connectorsQuery.data ?? []}
            services={servicesQuery.data ?? []}
            canSave={state.nodes.length > 0}
            saving={saveMutation.isPending}
            onSave={() => saveMutation.mutate()}
            onChange={(nodeId, patch) =>
              dispatch({ type: 'UPDATE_STEP', nodeId, patch })
            }
            onRemove={(nodeId) => dispatch({ type: 'REMOVE_NODE', nodeId })}
          />
        </div>
      </BuilderCollapsible>
    </section>
  )
}
