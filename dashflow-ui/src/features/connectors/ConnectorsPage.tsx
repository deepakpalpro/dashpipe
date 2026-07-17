import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import {
  createConnector,
  listConnectorTypes,
  listConnectors,
  updateConnector,
} from '../../api/resources'
import type {
  CreateConnectorRequest,
  TenantConnector,
  UpdateConnectorRequest,
} from '../../api/types'
import { useTenant } from '../../contexts/TenantContext'
import { ConnectorDetail } from './ConnectorDetail'
import { ConnectorForm } from './ConnectorForm'
import { ConnectorTreeNav } from './ConnectorTreeNav'
import { filterConnectors, paginateItems } from './connectorListFilter'
import {
  connectorSelectionLabel,
  connectorSelectionMatches,
  enrichConnectors,
  formatGroupLabel,
  type ConnectorTreeSelection,
} from './connectorTaxonomy'

const PAGE_SIZE = 20
const STATUS_OPTIONS = ['All', 'ACTIVE', 'INACTIVE', 'ERROR'] as const

export function ConnectorsPage() {
  const { tenantId } = useTenant()
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('All')
  const [treeSelection, setTreeSelection] = useState<ConnectorTreeSelection>({
    kind: 'all',
  })
  const [page, setPage] = useState(0)

  const connectorsQuery = useQuery({
    queryKey: ['connectors', tenantId],
    queryFn: () => listConnectors(tenantId),
  })

  const typesQuery = useQuery({
    queryKey: ['connector-types', tenantId],
    queryFn: () => listConnectorTypes(tenantId),
  })

  const createMutation = useMutation({
    mutationFn: (body: CreateConnectorRequest) =>
      createConnector(tenantId, body),
    onSuccess: (created) => {
      void queryClient.invalidateQueries({ queryKey: ['connectors', tenantId] })
      setCreating(false)
      setSelectedId(created.id)
      setSearch('')
      setStatusFilter('All')
      setTreeSelection({ kind: 'all' })
      setPage(0)
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({
      id,
      body,
    }: {
      id: string
      body: UpdateConnectorRequest
    }) => updateConnector(tenantId, id, body),
    onSuccess: (updated) => {
      void queryClient.invalidateQueries({ queryKey: ['connectors', tenantId] })
      setSelectedId(updated.id)
    },
  })

  const connectors = connectorsQuery.data ?? []
  const types = typesQuery.data ?? []
  const typeLabel = useMemo(() => {
    const map = new Map(types.map((t) => [t.id, t.displayName]))
    return (typeId: string) => map.get(typeId) ?? typeId
  }, [types])

  const enriched = useMemo(() => enrichConnectors(connectors), [connectors])

  const filtered = useMemo(() => {
    const byTree = enriched.filter((item) =>
      connectorSelectionMatches(item, treeSelection),
    )
    return filterConnectors(byTree, {
      search,
      status: statusFilter,
    })
  }, [enriched, treeSelection, search, statusFilter])

  const { pageItems, page: safePage, totalPages, total } = useMemo(
    () => paginateItems(filtered, page, PAGE_SIZE),
    [filtered, page],
  )

  useEffect(() => {
    setPage(0)
  }, [search, statusFilter, treeSelection, tenantId])

  useEffect(() => {
    if (page !== safePage) {
      setPage(safePage)
    }
  }, [page, safePage])

  const selected: TenantConnector | null =
    connectors.find((c) => c.id === selectedId) ??
    pageItems[0] ??
    filtered[0] ??
    null

  return (
    <section className="connectors-page" aria-label="Connectors">
      <div className="panel-header">
        <h1>Connectors</h1>
        <button type="button" onClick={() => setCreating(true)}>
          New
        </button>
      </div>

      <div className="connectors-layout">
        <ConnectorTreeNav
          items={enriched}
          selection={treeSelection}
          onSelect={setTreeSelection}
        />

        <div className="connectors-main">
          <div className="split-page connectors-split">
            <div className="split-list">
              <div className="list-toolbar" aria-label="Connector filters">
                <p className="catalog-selection muted" data-testid="connectors-path">
                  {connectorSelectionLabel(treeSelection)}
                </p>
                <label className="search-field list-toolbar-search">
                  <span className="sr-only">Search connectors</span>
                  <input
                    aria-label="Search connectors"
                    type="search"
                    placeholder="Search name or id…"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                  />
                </label>
                <label className="list-filter">
                  <span className="sr-only">Filter by status</span>
                  <select
                    aria-label="Filter by status"
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                  >
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s} value={s}>
                        {s === 'All' ? 'All statuses' : s}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <p className="muted list-count" data-testid="connectors-count">
                Showing {pageItems.length} of {total}
                {total !== connectors.length
                  ? ` (filtered from ${connectors.length})`
                  : ''}
              </p>

              {connectorsQuery.isLoading ? (
                <p className="muted">Loading…</p>
              ) : null}
              {connectorsQuery.isError ? (
                <p role="alert">Failed to load connectors</p>
              ) : null}
              {!connectorsQuery.isLoading && filtered.length === 0 ? (
                <p className="muted">No connectors match your filters.</p>
              ) : null}

              <ul className="entity-list">
                {pageItems.map((c) => (
                  <li key={c.id}>
                    <button
                      type="button"
                      className={
                        selected?.id === c.id
                          ? 'list-item active'
                          : 'list-item'
                      }
                      onClick={() => {
                        setSelectedId(c.id)
                        setCreating(false)
                      }}
                    >
                      <span className="list-item-title">{c.name}</span>
                      <span className="list-item-meta">
                        {c.category
                          ? `${c.category} · ${formatGroupLabel(c.group)}`
                          : typeLabel(c.connectorTypeId)}{' '}
                        · {c.status}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>

              {total > 0 ? (
                <div
                  className="list-pagination"
                  aria-label="Connector pagination"
                >
                  <button
                    type="button"
                    className="secondary"
                    disabled={safePage <= 0}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    Previous
                  </button>
                  <span className="muted" data-testid="connectors-page">
                    Page {safePage + 1} of {totalPages}
                  </span>
                  <button
                    type="button"
                    className="secondary"
                    disabled={safePage >= totalPages - 1}
                    onClick={() =>
                      setPage((p) => Math.min(totalPages - 1, p + 1))
                    }
                  >
                    Next
                  </button>
                </div>
              ) : null}
            </div>

            <div className="split-detail">
              {creating ? (
                <ConnectorForm
                  connectorTypes={types}
                  onCancel={() => setCreating(false)}
                  onSubmit={async (body) => {
                    await createMutation.mutateAsync(body)
                  }}
                />
              ) : (
                <ConnectorDetail
                  connector={selected}
                  saving={updateMutation.isPending}
                  onSave={async (id, body) => {
                    await updateMutation.mutateAsync({ id, body })
                  }}
                />
              )}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
