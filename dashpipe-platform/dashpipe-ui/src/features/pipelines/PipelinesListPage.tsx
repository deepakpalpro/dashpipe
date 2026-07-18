import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { archivePipeline, listPipelines } from '../../api/resources'
import type { PipelineResponse } from '../../api/types'
import { useTenant } from '../../contexts/TenantContext'
import { PipelineImportExportControls } from './PipelineImportExportControls'
import { PipelineStepsDetail } from './PipelineStepsDetail'

function isArchived(status: string) {
  return status.toUpperCase() === 'ARCHIVED'
}

export function PipelinesListPage() {
  const { tenantId } = useTenant()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const pipelinesQuery = useQuery({
    queryKey: ['pipelines', tenantId],
    queryFn: () => listPipelines(tenantId),
  })

  const archiveMutation = useMutation({
    mutationFn: (id: string) => archivePipeline(tenantId, id),
    onSuccess: (archived) => {
      void queryClient.invalidateQueries({ queryKey: ['pipelines', tenantId] })
      setMessage(`Archived ${archived.name}`)
      setSelectedId((cur) => (cur === archived.id ? null : cur))
    },
    onError: (err: Error) => {
      setMessage(`Archive failed: ${err.message}`)
    },
  })

  const pipelines = useMemo(
    () =>
      (pipelinesQuery.data ?? []).filter((p) => !isArchived(p.status)),
    [pipelinesQuery.data],
  )

  const selected: PipelineResponse | null =
    pipelines.find((p) => p.id === selectedId) ?? pipelines[0] ?? null

  return (
    <section className="split-page" aria-label="Pipelines">
      <div className="split-list">
        <div className="panel-header">
          <h1>Pipelines</h1>
          <div className="button-row">
            <PipelineImportExportControls
              tenantId={tenantId}
              onImported={(id, msg) => {
                setMessage(msg)
                setSelectedId(id)
                void queryClient.invalidateQueries({
                  queryKey: ['pipelines', tenantId],
                })
                navigate(`/pipelines/${id}`)
              }}
              onError={(msg) => setMessage(msg)}
            />
            <button
              type="button"
              onClick={() => navigate('/pipelines/new')}
            >
              New
            </button>
          </div>
        </div>
        {pipelinesQuery.isLoading ? <p className="muted">Loading…</p> : null}
        {pipelinesQuery.isError ? (
          <p role="alert">Failed to load pipelines</p>
        ) : null}
        {!pipelinesQuery.isLoading && pipelines.length === 0 ? (
          <p className="muted">No saved pipelines yet.</p>
        ) : null}
        <ul className="entity-list">
          {pipelines.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                className={
                  selected?.id === p.id ? 'list-item active' : 'list-item'
                }
                onClick={() => setSelectedId(p.id)}
              >
                <span className="list-item-title">{p.name}</span>
                <span className="list-item-meta">
                  {p.status} · v{p.version} · {p.steps?.length ?? 0} steps
                </span>
              </button>
            </li>
          ))}
        </ul>
      </div>

      <div className="split-detail">
        {selected ? (
          <div className="detail-panel-inner" data-testid="pipeline-detail">
            <div className="panel-header">
              <h2>{selected.name}</h2>
              <div className="button-row">
                <PipelineImportExportControls
                  tenantId={tenantId}
                  pipelineId={selected.id}
                  pipelineName={selected.name}
                  onImported={(id, msg) => {
                    setMessage(msg)
                    setSelectedId(id)
                    void queryClient.invalidateQueries({
                      queryKey: ['pipelines', tenantId],
                    })
                    navigate(`/pipelines/${id}`)
                  }}
                  onError={(msg) => setMessage(msg)}
                />
                <Link
                  className="button-link"
                  to={`/pipelines/${selected.id}`}
                >
                  Edit
                </Link>
                <button
                  type="button"
                  className="danger"
                  disabled={archiveMutation.isPending}
                  onClick={() => {
                    if (
                      window.confirm(
                        `Archive pipeline “${selected.name}”? It will leave the active list.`,
                      )
                    ) {
                      archiveMutation.mutate(selected.id)
                    }
                  }}
                >
                  Delete
                </button>
              </div>
            </div>
            <dl className="detail-grid">
              <div>
                <dt>Id</dt>
                <dd>
                  <code>{selected.id}</code>
                </dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{selected.status}</dd>
              </div>
              <div>
                <dt>Version</dt>
                <dd>{selected.version}</dd>
              </div>
              <div>
                <dt>Mode</dt>
                <dd>{selected.execution_mode ?? '—'}</dd>
              </div>
              <div>
                <dt>Visibility</dt>
                <dd>{selected.visibility ?? '—'}</dd>
              </div>
              <div>
                <dt>Steps</dt>
                <dd>{selected.steps?.length ?? 0}</dd>
              </div>
              {selected.description ? (
                <div className="detail-span">
                  <dt>Description</dt>
                  <dd>{selected.description}</dd>
                </div>
              ) : null}
              {selected.deployment_config &&
              Object.keys(selected.deployment_config).length > 0 ? (
                <div className="detail-span">
                  <dt>Deployment configuration</dt>
                  <dd>
                    <ul className="deployment-kv">
                      {Object.entries(selected.deployment_config).map(
                        ([k, v]) => (
                          <li key={k}>
                            <code>{k}</code>: {String(v ?? '')}
                          </li>
                        ),
                      )}
                    </ul>
                  </dd>
                </div>
              ) : null}
              {selected.execution_config &&
              Object.keys(selected.execution_config).length > 0 ? (
                <div className="detail-span">
                  <dt>Execution configuration</dt>
                  <dd>
                    <ul className="deployment-kv">
                      {Object.entries(selected.execution_config).map(
                        ([k, v]) => (
                          <li key={k}>
                            <code>{k}</code>: {String(v ?? '')}
                          </li>
                        ),
                      )}
                    </ul>
                  </dd>
                </div>
              ) : null}
            </dl>
            {selected.steps && selected.steps.length > 0 ? (
              <PipelineStepsDetail steps={selected.steps} />
            ) : (
              <p className="muted">No steps configured yet.</p>
            )}
            <div className="button-row">
              <Link
                className="button-link primary"
                to={`/pipelines/${selected.id}`}
              >
                Open in builder
              </Link>
              <Link
                className="button-link"
                to={`/observability?pipelineId=${encodeURIComponent(selected.id)}`}
              >
                Observability
              </Link>
            </div>
          </div>
        ) : (
          <p className="muted">Select a pipeline or create a new one.</p>
        )}
        {message ? (
          <p role="status" data-testid="pipeline-list-status">
            {message}
          </p>
        ) : null}
      </div>
    </section>
  )
}
