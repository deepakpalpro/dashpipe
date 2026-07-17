import pipeletPaths from '../../fixtures/pipeletPaths.json'
import type { PipeletCatalogEntry, PipeletCategory } from './catalogFilter'

/** System vs tenant-registered catalog ownership. */
export type PipeletScope = 'system' | 'tenant'

/**
 * Domain group under a category (matches pipelets/{source|transformer|destination}/&lt;group&gt;/).
 * Tenant registrations without a path use {@link TENANT_CUSTOM_GROUP}.
 */
export type PipeletGroup = string

export const TENANT_CUSTOM_GROUP = 'custom'

/** Canonical System groups by UI category (filesystem layout). */
export const SYSTEM_GROUPS: Record<PipeletCategory, readonly string[]> = {
  Source: ['database', 'http', 'messaging', 'storage', 'trigger'],
  Processor: [
    'enrich',
    'extract',
    'filter',
    'quality',
    'security',
    'structure',
    'transform',
  ],
  Destination: [
    'database',
    'messaging',
    'notify',
    'saas',
    'storage',
    'util',
  ],
}

export const PIPELET_CATEGORIES: PipeletCategory[] = [
  'Source',
  'Processor',
  'Destination',
]

const ROOT_TO_CATEGORY: Record<string, PipeletCategory> = {
  source: 'Source',
  transformer: 'Processor',
  destination: 'Destination',
}

/** Normalize JSON module interop (default export vs bare object). */
function asPathMap(mod: unknown): Record<string, string> {
  if (!mod || typeof mod !== 'object') {
    return {}
  }
  const record = mod as Record<string, unknown>
  if (
    'default' in record &&
    record.default &&
    typeof record.default === 'object' &&
    !Array.isArray(record.default)
  ) {
    return record.default as Record<string, string>
  }
  return record as Record<string, string>
}

const PATHS = asPathMap(pipeletPaths)

export function formatGroupLabel(group: string): string {
  if (!group) {
    return 'Other'
  }
  if (group === TENANT_CUSTOM_GROUP) {
    return 'Custom'
  }
  return group
    .split(/[-_]/)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function resolvePathTaxonomy(path: string | undefined): {
  category?: PipeletCategory
  group?: string
} {
  if (!path || typeof path !== 'string') {
    return {}
  }
  const parts = path.split('/').filter(Boolean)
  if (parts.length < 2) {
    return {}
  }
  const root = parts[0]!
  const group = parts[1]!
  return {
    category: ROOT_TO_CATEGORY[root],
    group,
  }
}

/**
 * Enrich a catalog entry with scope/group from PATHS.json (system) or defaults (tenant).
 * PATHS always wins for known system ids so stale `custom` placeholders get corrected.
 */
export function enrichPipeletTaxonomy(
  entry: PipeletCatalogEntry,
): PipeletCatalogEntry {
  const path = PATHS[entry.id]
  if (path) {
    const { group } = resolvePathTaxonomy(path)
    return {
      ...entry,
      scope: entry.scope ?? 'system',
      group: group ?? entry.group ?? TENANT_CUSTOM_GROUP,
    }
  }
  return {
    ...entry,
    scope: entry.scope ?? 'tenant',
    group: entry.group ?? TENANT_CUSTOM_GROUP,
  }
}

export function enrichCatalog(
  entries: PipeletCatalogEntry[],
): PipeletCatalogEntry[] {
  return entries.map(enrichPipeletTaxonomy)
}

/** Groups present for a scope+category, ordered (system canon first, then extras). */
export function groupsFor(
  items: PipeletCatalogEntry[],
  scope: PipeletScope,
  category: PipeletCategory,
): string[] {
  const seen = new Set<string>()
  for (const item of items) {
    if ((item.scope ?? 'system') !== scope) {
      continue
    }
    if (item.category !== category) {
      continue
    }
    seen.add(item.group ?? TENANT_CUSTOM_GROUP)
  }
  const preferred =
    scope === 'system' ? [...SYSTEM_GROUPS[category]] : [TENANT_CUSTOM_GROUP]
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

export type CatalogTreeSelection =
  | { kind: 'all' }
  | { kind: 'scope'; scope: PipeletScope }
  | { kind: 'category'; scope: PipeletScope; category: PipeletCategory }
  | {
      kind: 'group'
      scope: PipeletScope
      category: PipeletCategory
      group: string
    }

export function selectionMatches(
  item: PipeletCatalogEntry,
  selection: CatalogTreeSelection,
): boolean {
  const scope = item.scope ?? 'system'
  const group = item.group ?? TENANT_CUSTOM_GROUP
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
      return true
  }
}
