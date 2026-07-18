import { useState } from 'react'
import {
  connectorGroupsFor,
  formatGroupLabel,
  PIPELET_CATEGORIES,
  type ConnectorKind,
  type ConnectorTreeSelection,
  type EnrichedConnector,
} from './connectorTaxonomy'

type Props = {
  items: EnrichedConnector[]
  selection: ConnectorTreeSelection
  onSelect: (selection: ConnectorTreeSelection) => void
}

function countFor(
  items: EnrichedConnector[],
  selection: ConnectorTreeSelection,
): number {
  return items.filter((item) => {
    const group = item.group || 'custom'
    switch (selection.kind) {
      case 'all':
        return true
      case 'category':
        return item.category === selection.category
      case 'group':
        return (
          item.category === selection.category && group === selection.group
        )
      case 'other':
        return !item.category
      default:
        return false
    }
  }).length
}

function isSelected(
  current: ConnectorTreeSelection,
  candidate: ConnectorTreeSelection,
): boolean {
  if (current.kind !== candidate.kind) {
    return false
  }
  if (candidate.kind === 'all' || candidate.kind === 'other') {
    return true
  }
  if (candidate.kind === 'category' && current.kind === 'category') {
    return current.category === candidate.category
  }
  if (candidate.kind === 'group' && current.kind === 'group') {
    return (
      current.category === candidate.category &&
      current.group === candidate.group
    )
  }
  return false
}

function CategoryBranch({
  category,
  items,
  selection,
  onSelect,
}: {
  category: ConnectorKind
  items: EnrichedConnector[]
  selection: ConnectorTreeSelection
  onSelect: (selection: ConnectorTreeSelection) => void
}) {
  const catSel: ConnectorTreeSelection = { kind: 'category', category }
  const groups = connectorGroupsFor(items, category)
  const n = countFor(items, catSel)
  const [expanded, setExpanded] = useState(false)

  return (
    <li className="catalog-tree-category">
      <button
        type="button"
        className={
          isSelected(selection, catSel)
            ? 'catalog-tree-btn active'
            : 'catalog-tree-btn'
        }
        aria-label={category}
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
            const groupSel: ConnectorTreeSelection = {
              kind: 'group',
              category,
              group,
            }
            return (
              <li key={`${category}-${group}`}>
                <button
                  type="button"
                  className={
                    isSelected(selection, groupSel)
                      ? 'catalog-tree-btn nested active'
                      : 'catalog-tree-btn nested'
                  }
                  aria-label={`${category} ${formatGroupLabel(group)}`}
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

export function ConnectorTreeNav({ items, selection, onSelect }: Props) {
  const allSel: ConnectorTreeSelection = { kind: 'all' }
  const otherSel: ConnectorTreeSelection = { kind: 'other' }
  const otherCount = countFor(items, otherSel)

  return (
    <nav className="catalog-tree" aria-label="Connector categories">
      <button
        type="button"
        className={
          isSelected(selection, allSel)
            ? 'catalog-tree-btn active'
            : 'catalog-tree-btn'
        }
        onClick={() => onSelect(allSel)}
      >
        <span>All connectors</span>
        <span className="catalog-tree-count">{items.length}</span>
      </button>
      <ul className="catalog-tree-scopes">
        {PIPELET_CATEGORIES.map((category) => (
          <CategoryBranch
            key={category}
            category={category}
            items={items}
            selection={selection}
            onSelect={onSelect}
          />
        ))}
        {otherCount > 0 ? (
          <li className="catalog-tree-category">
            <button
              type="button"
              className={
                isSelected(selection, otherSel)
                  ? 'catalog-tree-btn active'
                  : 'catalog-tree-btn'
              }
              aria-label="Other"
              onClick={() => onSelect(otherSel)}
            >
              <span className="catalog-tree-label">
                <span className="catalog-tree-toggle" aria-hidden="true" />
                Other
              </span>
              <span className="catalog-tree-count">{otherCount}</span>
            </button>
          </li>
        ) : null}
      </ul>
    </nav>
  )
}
