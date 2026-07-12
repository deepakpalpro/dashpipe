import { useMemo, useState } from 'react'
import {
  activePipelets,
  catalogFilter,
  type PipeletCatalogEntry,
  type PipeletCategory,
} from '../../pipelets/catalogFilter'

export const PALETTE_PREVIEW_LIMIT = 5

export const PALETTE_CATEGORIES: PipeletCategory[] = [
  'Source',
  'Processor',
  'Destination',
]

type Props = {
  items: PipeletCatalogEntry[]
  onAdd: (item: PipeletCatalogEntry) => void
  previewLimit?: number
}

export function PipeletPalette({
  items,
  onAdd,
  previewLimit = PALETTE_PREVIEW_LIMIT,
}: Props) {
  const [search, setSearch] = useState('')
  const [expanded, setExpanded] = useState<Record<PipeletCategory, boolean>>({
    Source: false,
    Processor: false,
    Destination: false,
  })

  const paletteItems = useMemo(() => activePipelets(items), [items])

  const filtered = useMemo(
    () => catalogFilter(paletteItems, { search, status: 'Active' }),
    [paletteItems, search],
  )

  const byCategory = useMemo(() => {
    const map: Record<PipeletCategory, PipeletCatalogEntry[]> = {
      Source: [],
      Processor: [],
      Destination: [],
    }
    for (const item of filtered) {
      map[item.category].push(item)
    }
    return map
  }, [filtered])

  const searching = search.trim().length > 0

  return (
    <aside className="builder-palette" aria-label="Pipelet palette">
      <h2>Palette</h2>
      <label className="palette-search">
        <span className="sr-only">Search pipelets</span>
        <input
          type="search"
          aria-label="Search pipelets"
          placeholder="Search to add…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </label>

      {paletteItems.length === 0 ? (
        <p className="muted palette-empty">
          No active pipelets. Register a pipelet with an image, or mark catalog
          entries active when a runtime is available.
        </p>
      ) : filtered.length === 0 ? (
        <p className="muted palette-empty">
          No pipelets match “{search.trim()}”.
        </p>
      ) : null}

      <div className="palette-categories">
        {PALETTE_CATEGORIES.map((category) => {
          const all = byCategory[category]
          if (all.length === 0) {
            return null
          }
          const isExpanded = searching || expanded[category]
          const visible = isExpanded ? all : all.slice(0, previewLimit)
          const hiddenCount = all.length - visible.length

          return (
            <section
              key={category}
              className="palette-category"
              aria-label={`${category} pipelets`}
            >
              <header className="palette-category-header">
                <h3>{category}</h3>
                <span className="palette-count">{all.length}</span>
              </header>
              <ul className="palette-list">
                {visible.map((item) => (
                  <li key={item.id}>
                    <button
                      type="button"
                      className="palette-item"
                      onClick={() => onAdd(item)}
                      title={item.description}
                    >
                      <span className="list-item-title">{item.name}</span>
                      <span className="list-item-meta">
                        {item.runtime} · v{item.version}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
              {!searching && all.length > previewLimit ? (
                <button
                  type="button"
                  className="palette-more secondary"
                  onClick={() =>
                    setExpanded((prev) => ({
                      ...prev,
                      [category]: !prev[category],
                    }))
                  }
                >
                  {isExpanded ? `Show less` : `Show ${hiddenCount} more`}
                </button>
              ) : null}
            </section>
          )
        })}
      </div>
    </aside>
  )
}
