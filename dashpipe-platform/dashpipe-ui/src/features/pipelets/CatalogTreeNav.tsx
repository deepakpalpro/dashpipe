import { useState } from 'react'
import type { PipeletCatalogEntry, PipeletCategory } from './catalogFilter'
import {
  formatGroupLabel,
  groupsFor,
  PIPELET_CATEGORIES,
  type CatalogTreeSelection,
  type PipeletScope,
} from './pipeletTaxonomy'

type Props = {
  items: PipeletCatalogEntry[]
  selection: CatalogTreeSelection
  onSelect: (selection: CatalogTreeSelection) => void
}

function countFor(
  items: PipeletCatalogEntry[],
  selection: CatalogTreeSelection,
): number {
  return items.filter((item) => {
    const scope = item.scope ?? 'system'
    const group = item.group ?? 'custom'
    switch (selection.kind) {
      case 'all':
        return true
      case 'scope':
        return scope === selection.scope
      case 'category':
        return scope === selection.scope && item.category === selection.category
      case 'group':
        return (
          scope === selection.scope &&
          item.category === selection.category &&
          group === selection.group
        )
      default:
        return false
    }
  }).length
}

function isSelected(
  current: CatalogTreeSelection,
  candidate: CatalogTreeSelection,
): boolean {
  if (current.kind !== candidate.kind) {
    return false
  }
  if (candidate.kind === 'all') {
    return true
  }
  if (candidate.kind === 'scope' && current.kind === 'scope') {
    return current.scope === candidate.scope
  }
  if (candidate.kind === 'category' && current.kind === 'category') {
    return (
      current.scope === candidate.scope &&
      current.category === candidate.category
    )
  }
  if (candidate.kind === 'group' && current.kind === 'group') {
    return (
      current.scope === candidate.scope &&
      current.category === candidate.category &&
      current.group === candidate.group
    )
  }
  return false
}

function ScopeBranch({
  scope,
  label,
  items,
  selection,
  onSelect,
}: {
  scope: PipeletScope
  label: string
  items: PipeletCatalogEntry[]
  selection: CatalogTreeSelection
  onSelect: (selection: CatalogTreeSelection) => void
}) {
  const scopeSel: CatalogTreeSelection = { kind: 'scope', scope }
  const [expanded, setExpanded] = useState(true)
  return (
    <li className="catalog-tree-scope">
      <button
        type="button"
        className={
          isSelected(selection, scopeSel)
            ? 'catalog-tree-btn active'
            : 'catalog-tree-btn'
        }
        aria-label={label}
        aria-expanded={expanded}
        onClick={() => {
          onSelect(scopeSel)
          setExpanded((current) => !current)
        }}
      >
        <span className="catalog-tree-label">
          <span className="catalog-tree-toggle" aria-hidden="true">
            {expanded ? '▾' : '▸'}
          </span>
          {label}
        </span>
        <span className="catalog-tree-count">{countFor(items, scopeSel)}</span>
      </button>
      {expanded ? (
        <ul className="catalog-tree-categories">
          {PIPELET_CATEGORIES.map((category) => (
            <CategoryBranch
              key={`${scope}-${category}`}
              scope={scope}
              category={category}
              items={items}
              selection={selection}
              onSelect={onSelect}
            />
          ))}
        </ul>
      ) : null}
    </li>
  )
}

function CategoryBranch({
  scope,
  category,
  items,
  selection,
  onSelect,
}: {
  scope: PipeletScope
  category: PipeletCategory
  items: PipeletCatalogEntry[]
  selection: CatalogTreeSelection
  onSelect: (selection: CatalogTreeSelection) => void
}) {
  const catSel: CatalogTreeSelection = { kind: 'category', scope, category }
  const groups = groupsFor(items, scope, category)
  const n = countFor(items, catSel)
  const [expanded, setExpanded] = useState(false)
  if (n === 0 && scope === 'tenant') {
    // Still show empty Tenant categories so users know where registrations land.
  }
  return (
    <li className="catalog-tree-category">
      <button
        type="button"
        className={
          isSelected(selection, catSel)
            ? 'catalog-tree-btn active'
            : 'catalog-tree-btn'
        }
        aria-label={`${scope === 'system' ? 'System' : 'Tenant Custom'} ${category}`}
        aria-expanded={groups.length > 0 ? expanded : undefined}
        onClick={() => {
          onSelect(catSel)
          if (groups.length > 0) {
            setExpanded((current) => !current)
          }
        }}
      >
        <span className="catalog-tree-label">
          {groups.length > 0 ? (
            <span className="catalog-tree-toggle" aria-hidden="true">
              {expanded ? '▾' : '▸'}
            </span>
          ) : (
            <span className="catalog-tree-toggle" aria-hidden="true" />
          )}
          {category}
        </span>
        <span className="catalog-tree-count">{n}</span>
      </button>
      {groups.length > 0 && expanded ? (
        <ul className="catalog-tree-groups">
          {groups.map((group) => {
            const groupSel: CatalogTreeSelection = {
              kind: 'group',
              scope,
              category,
              group,
            }
            return (
              <li key={`${scope}-${category}-${group}`}>
                <button
                  type="button"
                  className={
                    isSelected(selection, groupSel)
                      ? 'catalog-tree-btn nested active'
                      : 'catalog-tree-btn nested'
                  }
                  aria-label={`${scope === 'system' ? 'System' : 'Tenant Custom'} ${category} ${formatGroupLabel(group)}`}
                  onClick={() => onSelect(groupSel)}
                >
                  <span>{formatGroupLabel(group)}</span>
                  <span className="catalog-tree-count">
                    {countFor(items, groupSel)}
                  </span>
                </button>
              </li>
            )
          })}
        </ul>
      ) : null}
    </li>
  )
}

export function CatalogTreeNav({ items, selection, onSelect }: Props) {
  const allSel: CatalogTreeSelection = { kind: 'all' }
  return (
    <nav className="catalog-tree" aria-label="Pipelet categories">
      <button
        type="button"
        className={
          isSelected(selection, allSel)
            ? 'catalog-tree-btn active'
            : 'catalog-tree-btn'
        }
        onClick={() => onSelect(allSel)}
      >
        <span>All pipelets</span>
        <span className="catalog-tree-count">{items.length}</span>
      </button>
      <ul className="catalog-tree-scopes">
        <ScopeBranch
          scope="system"
          label="System"
          items={items}
          selection={selection}
          onSelect={onSelect}
        />
        <ScopeBranch
          scope="tenant"
          label="Tenant Custom"
          items={items}
          selection={selection}
          onSelect={onSelect}
        />
      </ul>
    </nav>
  )
}
