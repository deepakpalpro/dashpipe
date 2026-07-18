import { useMemo, useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import {
  catalogFilter,
  type PipeletCatalogEntry,
  type PipeletCategory,
  type PipeletStatusFilter,
} from './catalogFilter'
import { CatalogTreeNav } from './CatalogTreeNav'
import { usePipeletCatalog } from './PipeletCatalogContext'
import { PipeletCard } from './PipeletCard'
import { PipeletDetailModal } from './PipeletDetailModal'
import {
  RegisterPipeletModal,
  type RegisterPipeletInput,
} from './RegisterPipeletModal'
import {
  enrichCatalog,
  enrichPipeletTaxonomy,
  formatGroupLabel,
  selectionMatches,
  TENANT_CUSTOM_GROUP,
  type CatalogTreeSelection,
} from './pipeletTaxonomy'

const STATUS_FILTERS: Array<Exclude<PipeletStatusFilter, 'All'>> = [
  'Active',
  'Inactive',
]

type Props = {
  catalog?: PipeletCatalogEntry[]
  onRegister?: (input: RegisterPipeletInput) => Promise<void> | void
}

function selectionLabel(selection: CatalogTreeSelection): string {
  switch (selection.kind) {
    case 'all':
      return 'All pipelets'
    case 'scope':
      return selection.scope === 'system' ? 'System' : 'Tenant Custom'
    case 'category':
      return `${selection.scope === 'system' ? 'System' : 'Tenant Custom'} · ${selection.category}`
    case 'group':
      return `${selection.scope === 'system' ? 'System' : 'Tenant Custom'} · ${selection.category} · ${formatGroupLabel(selection.group)}`
    default:
      return 'Pipelets'
  }
}

export function PipeletsCatalogPage({
  catalog: catalogProp,
  onRegister,
}: Props) {
  const { isAdmin } = useAuth()
  const { catalog: contextCatalog, activatePipelets } = usePipeletCatalog()
  const catalog = catalogProp ?? contextCatalog
  const [treeSelection, setTreeSelection] = useState<CatalogTreeSelection>({
    kind: 'all',
  })
  const [status, setStatus] = useState<PipeletStatusFilter>('Active')
  const [search, setSearch] = useState('')
  const [registerOpen, setRegisterOpen] = useState(false)
  const [selected, setSelected] = useState<PipeletCatalogEntry | null>(null)
  const [registered, setRegistered] = useState<PipeletCatalogEntry[]>([])

  const items = useMemo(
    () => [...registered, ...enrichCatalog(catalog)],
    [catalog, registered],
  )

  const filtered = useMemo(() => {
    const byTree = items.filter((item) =>
      selectionMatches(item, treeSelection),
    )
    return catalogFilter(byTree, { search, status })
  }, [items, treeSelection, search, status])

  async function handleRegister(input: RegisterPipeletInput) {
    if (onRegister) {
      await onRegister(input)
    } else {
      await fetch('/api/v1/pipelets/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(input),
      })
    }
    const id = `plet-${input.name.toLowerCase().replace(/\s+/g, '-')}`
    activatePipelets([id])
    const entry = enrichPipeletTaxonomy({
      id,
      name: input.name,
      category: input.category as PipeletCategory,
      version: '0.1.0',
      runtime: input.mode === 'runtimeBinary' ? 'Binary' : 'Java',
      description: `Registered via ${input.mode}: ${input.imageRef}`,
      active: true,
      scope: 'tenant',
      group: input.group?.trim() || TENANT_CUSTOM_GROUP,
      imageRef: input.imageRef,
    })
    setRegistered((prev) => [entry, ...prev])
    setTreeSelection({
      kind: 'group',
      scope: 'tenant',
      category: entry.category,
      group: entry.group ?? TENANT_CUSTOM_GROUP,
    })
  }

  return (
    <section className="catalog-page" aria-label="Pipelets catalog">
      <div className="panel-header">
        <h1>Pipelets</h1>
        {isAdmin ? (
          <button type="button" onClick={() => setRegisterOpen(true)}>
            Register Pipelet
          </button>
        ) : null}
      </div>

      <div className="catalog-layout">
        <CatalogTreeNav
          items={items}
          selection={treeSelection}
          onSelect={setTreeSelection}
        />

        <div className="catalog-main">
          <div className="catalog-toolbar">
            <p className="catalog-selection muted" data-testid="catalog-path">
              {selectionLabel(treeSelection)}
            </p>
            <div
              className="status-slider"
              role="radiogroup"
              aria-label="Pipelet status"
            >
              {STATUS_FILTERS.map((s) => (
                <label
                  key={s}
                  className={
                    status === s
                      ? 'status-slider-option selected'
                      : 'status-slider-option'
                  }
                >
                  <input
                    type="radio"
                    name="pipelet-status"
                    value={s}
                    checked={status === s}
                    onChange={() => setStatus(s)}
                  />
                  <span>{s}</span>
                </label>
              ))}
            </div>
            <label className="search-field">
              <span className="sr-only">Search pipelets</span>
              <input
                aria-label="Search pipelets"
                placeholder="Search name or id…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </label>
          </div>

          <p className="muted" data-testid="catalog-count">
            Showing {filtered.length} of {items.length}
          </p>

          <div className="pipelet-grid">
            {filtered.map((p) => (
              <PipeletCard key={p.id} pipelet={p} onSelect={setSelected} />
            ))}
          </div>
        </div>
      </div>

      <PipeletDetailModal
        pipelet={selected}
        onClose={() => setSelected(null)}
      />

      <RegisterPipeletModal
        open={registerOpen}
        onClose={() => setRegisterOpen(false)}
        onSubmit={handleRegister}
      />
    </section>
  )
}
