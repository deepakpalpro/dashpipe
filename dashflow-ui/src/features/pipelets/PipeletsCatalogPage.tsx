import { useMemo, useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import {
  catalogFilter,
  type PipeletCatalogEntry,
  type PipeletCategory,
  type PipeletStatusFilter,
} from './catalogFilter'
import { PIPELET_FIXTURE } from './fixture'
import { PipeletCard } from './PipeletCard'
import { PipeletDetailModal } from './PipeletDetailModal'
import {
  RegisterPipeletModal,
  type RegisterPipeletInput,
} from './RegisterPipeletModal'

const CATEGORIES: Array<PipeletCategory | 'All'> = [
  'All',
  'Source',
  'Processor',
  'Destination',
]

const STATUS_FILTERS: Array<Exclude<PipeletStatusFilter, 'All'>> = [
  'Active',
  'Inactive',
]

type Props = {
  catalog?: PipeletCatalogEntry[]
  onRegister?: (input: RegisterPipeletInput) => Promise<void> | void
}

export function PipeletsCatalogPage({
  catalog = PIPELET_FIXTURE,
  onRegister,
}: Props) {
  const { isAdmin } = useAuth()
  const [category, setCategory] = useState<PipeletCategory | 'All'>('All')
  const [status, setStatus] = useState<PipeletStatusFilter>('Active')
  const [search, setSearch] = useState('')
  const [registerOpen, setRegisterOpen] = useState(false)
  const [selected, setSelected] = useState<PipeletCatalogEntry | null>(null)
  const [items, setItems] = useState(catalog)

  const filtered = useMemo(
    () => catalogFilter(items, { category, search, status }),
    [items, category, search, status],
  )

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
    setItems((prev) => [
      {
        id: `plet-${input.name.toLowerCase().replace(/\s+/g, '-')}`,
        name: input.name,
        category: input.category as PipeletCategory,
        version: '0.1.0',
        runtime: input.mode === 'runtimeBinary' ? 'Binary' : 'Java',
        description: `Registered via ${input.mode}: ${input.imageRef}`,
        // Newly registered pipelets include an image/binary ref → active for palette.
        active: true,
      },
      ...prev,
    ])
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

      <div className="catalog-toolbar">
        <div className="tab-row" role="tablist" aria-label="Pipelet category">
          {CATEGORIES.map((c) => (
            <button
              key={c}
              type="button"
              role="tab"
              aria-selected={category === c}
              className={category === c ? 'tab active' : 'tab'}
              onClick={() => setCategory(c)}
            >
              {c}
            </button>
          ))}
        </div>
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
