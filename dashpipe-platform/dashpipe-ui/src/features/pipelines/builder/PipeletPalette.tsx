import { useMemo, useState } from 'react'
import {
  activePipelets,
  catalogFilter,
  type PipeletCatalogEntry,
  type PipeletCategory,
} from '../../pipelets/catalogFilter'
import {
  formatGroupLabel,
  groupsFor,
  PIPELET_CATEGORIES,
  TENANT_CUSTOM_GROUP,
} from '../../pipelets/pipeletTaxonomy'

export const PALETTE_PREVIEW_LIMIT = 5

export const PALETTE_CATEGORIES: PipeletCategory[] = PIPELET_CATEGORIES

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
  const [expanded, setExpanded] = useState<Record<string, boolean>>({})

  const paletteItems = useMemo(() => activePipelets(items), [items])

  const filtered = useMemo(
    () => catalogFilter(paletteItems, { search, status: 'Active' }),
    [paletteItems, search],
  )

  const searching = search.trim().length > 0

  const scopes = useMemo(() => {
    const hasSystem = filtered.some((p) => (p.scope ?? 'system') === 'system')
    const hasTenant = filtered.some((p) => p.scope === 'tenant')
    const out: Array<{ scope: 'system' | 'tenant'; label: string }> = []
    if (hasSystem) {
      out.push({ scope: 'system', label: 'System' })
    }
    if (hasTenant) {
      out.push({ scope: 'tenant', label: 'Tenant Custom' })
    }
    if (out.length === 0) {
      out.push({ scope: 'system', label: 'System' })
    }
    return out
  }, [filtered])

  function toggle(key: string) {
    setExpanded((prev) => ({ ...prev, [key]: !prev[key] }))
  }

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
        {scopes.map(({ scope, label }) => {
          const scopeItems = filtered.filter(
            (p) => (p.scope ?? 'system') === scope,
          )
          if (scopeItems.length === 0) {
            return null
          }
          return (
            <section
              key={scope}
              className="palette-scope"
              aria-label={`${label} pipelets`}
            >
              <header className="palette-scope-header">
                <h3>{label}</h3>
                <span className="palette-count">{scopeItems.length}</span>
              </header>

              {PALETTE_CATEGORIES.map((category) => {
                const all = scopeItems.filter((p) => p.category === category)
                if (all.length === 0) {
                  return null
                }
                const catKey = `${scope}:${category}`
                const groups = groupsFor(scopeItems, scope, category)
                const showGroups = groups.length > 1 || scope === 'system'

                return (
                  <section
                    key={catKey}
                    className="palette-category"
                    aria-label={`${category} pipelets`}
                  >
                    <header className="palette-category-header">
                      <h4>{category}</h4>
                      <span className="palette-count">{all.length}</span>
                    </header>

                    {showGroups
                      ? groups.map((group) => {
                          const groupItems = all.filter(
                            (p) =>
                              (p.group ?? TENANT_CUSTOM_GROUP) === group,
                          )
                          if (groupItems.length === 0) {
                            return null
                          }
                          const groupKey = `${catKey}:${group}`
                          const isExpanded =
                            searching || expanded[groupKey] === true
                          const visible = isExpanded
                            ? groupItems
                            : groupItems.slice(0, previewLimit)
                          const hiddenCount = groupItems.length - visible.length

                          return (
                            <div
                              key={groupKey}
                              className="palette-group"
                              aria-label={`${formatGroupLabel(group)} pipelets`}
                            >
                              <header className="palette-group-header">
                                <h5>{formatGroupLabel(group)}</h5>
                                <span className="palette-count">
                                  {groupItems.length}
                                </span>
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
                                      <span className="list-item-title">
                                        {item.name}
                                      </span>
                                      <span className="list-item-meta">
                                        {item.runtime} · v{item.version}
                                      </span>
                                    </button>
                                  </li>
                                ))}
                              </ul>
                              {!searching &&
                              groupItems.length > previewLimit ? (
                                <button
                                  type="button"
                                  className="palette-more secondary"
                                  onClick={() => toggle(groupKey)}
                                >
                                  {isExpanded
                                    ? 'Show less'
                                    : `Show ${hiddenCount} more`}
                                </button>
                              ) : null}
                            </div>
                          )
                        })
                      : (() => {
                          const isExpanded =
                            searching || expanded[catKey] === true
                          const visible = isExpanded
                            ? all
                            : all.slice(0, previewLimit)
                          const hiddenCount = all.length - visible.length
                          return (
                            <>
                              <ul className="palette-list">
                                {visible.map((item) => (
                                  <li key={item.id}>
                                    <button
                                      type="button"
                                      className="palette-item"
                                      onClick={() => onAdd(item)}
                                      title={item.description}
                                    >
                                      <span className="list-item-title">
                                        {item.name}
                                      </span>
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
                                  onClick={() => toggle(catKey)}
                                >
                                  {isExpanded
                                    ? 'Show less'
                                    : `Show ${hiddenCount} more`}
                                </button>
                              ) : null}
                            </>
                          )
                        })()}
                  </section>
                )
              })}
            </section>
          )
        })}
      </div>
    </aside>
  )
}
