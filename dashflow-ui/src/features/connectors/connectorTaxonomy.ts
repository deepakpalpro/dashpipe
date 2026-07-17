import type { TenantConnector } from '../../api/types'
import type { PipeletCatalogEntry, PipeletCategory } from '../pipelets/catalogFilter'
import { getPipeletCatalog } from '../pipelets/fixture'
import {
  formatGroupLabel,
  PIPELET_CATEGORIES,
  SYSTEM_GROUPS,
  TENANT_CUSTOM_GROUP,
} from '../pipelets/pipeletTaxonomy'

export { formatGroupLabel, PIPELET_CATEGORIES }

export type ConnectorKind = PipeletCategory

export type ConnectorTaxonomy = {
  category?: ConnectorKind
  group: string
  pipeletId?: string
}

export type EnrichedConnector = TenantConnector & ConnectorTaxonomy

export type ConnectorTreeSelection =
  | { kind: 'all' }
  | { kind: 'category'; category: ConnectorKind }
  | { kind: 'group'; category: ConnectorKind; group: string }
  | { kind: 'other' }

const PLET_IN_NAME = /\((plet-[a-z0-9-]+)\)/i

/** Resolve linked pipelet id from connector id (`conn-plet-…`) or name `(plet-…)`. */
export function pipeletIdFromConnector(connector: {
  id: string
  name: string
}): string | undefined {
  if (connector.id.startsWith('conn-plet-')) {
    return connector.id.slice('conn-'.length)
  }
  const match = PLET_IN_NAME.exec(connector.name)
  return match?.[1]?.toLowerCase()
}

function catalogById(
  catalog: PipeletCatalogEntry[] = getPipeletCatalog(),
): Map<string, PipeletCatalogEntry> {
  return new Map(catalog.map((p) => [p.id, p]))
}

export function resolveConnectorTaxonomy(
  connector: { id: string; name: string },
  catalog: PipeletCatalogEntry[] = getPipeletCatalog(),
): ConnectorTaxonomy {
  const pipeletId = pipeletIdFromConnector(connector)
  const pipelet = pipeletId ? catalogById(catalog).get(pipeletId) : undefined
  if (!pipelet) {
    return { group: TENANT_CUSTOM_GROUP }
  }
  return {
    pipeletId,
    category: pipelet.category,
    group: pipelet.group ?? TENANT_CUSTOM_GROUP,
  }
}

export function enrichConnector(
  connector: TenantConnector,
  catalog?: PipeletCatalogEntry[],
): EnrichedConnector {
  return {
    ...connector,
    ...resolveConnectorTaxonomy(connector, catalog),
  }
}

export function enrichConnectors(
  connectors: TenantConnector[],
  catalog?: PipeletCatalogEntry[],
): EnrichedConnector[] {
  const cat = catalog ?? getPipeletCatalog()
  return connectors.map((c) => enrichConnector(c, cat))
}

export function connectorGroupsFor(
  items: EnrichedConnector[],
  category: ConnectorKind,
): string[] {
  const seen = new Set<string>()
  for (const item of items) {
    if (item.category !== category) {
      continue
    }
    seen.add(item.group || TENANT_CUSTOM_GROUP)
  }
  const preferred = [...SYSTEM_GROUPS[category]]
  const ordered: string[] = []
  for (const g of preferred) {
    if (seen.has(g)) {
      ordered.push(g)
      seen.delete(g)
    }
  }
  for (const g of [...seen].sort()) {
    ordered.push(g)
  }
  return ordered
}

export function connectorSelectionMatches(
  item: EnrichedConnector,
  selection: ConnectorTreeSelection,
): boolean {
  const group = item.group || TENANT_CUSTOM_GROUP
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
      return true
  }
}

export function connectorSelectionLabel(
  selection: ConnectorTreeSelection,
): string {
  switch (selection.kind) {
    case 'all':
      return 'All connectors'
    case 'category':
      return selection.category
    case 'group':
      return `${selection.category} · ${formatGroupLabel(selection.group)}`
    case 'other':
      return 'Other'
    default:
      return 'Connectors'
  }
}
