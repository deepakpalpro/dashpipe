import { describe, expect, it } from 'vitest'
import {
  activePipelets,
  catalogFilter,
  type PipeletCatalogEntry,
} from './catalogFilter'
import {
  enrichPipeletTaxonomy,
  resolvePathTaxonomy,
  selectionMatches,
} from './pipeletTaxonomy'

const SAMPLE: PipeletCatalogEntry[] = [
  {
    id: 'plet-a',
    name: 'REST Source',
    category: 'Source',
    version: '1.0.0',
    runtime: 'Java',
    description: 'HTTP ingress',
    active: false,
    scope: 'system',
    group: 'http',
  },
  {
    id: 'plet-b',
    name: 'JSON Transform',
    category: 'Processor',
    version: '1.0.0',
    runtime: 'Java',
    description: 'Map fields',
    active: true,
    scope: 'system',
    group: 'transform',
  },
  {
    id: 'plet-c',
    name: 'S3 Destination',
    category: 'Destination',
    version: '1.0.0',
    runtime: 'Java',
    description: 'Write objects',
    scope: 'tenant',
    group: 'storage',
  },
]

describe('catalogFilter', () => {
  it('filters by category Source', () => {
    const result = catalogFilter(SAMPLE, { category: 'Source' })
    expect(result).toHaveLength(1)
    expect(result[0]?.id).toBe('plet-a')
  })

  it('searches by name substring', () => {
    const result = catalogFilter(SAMPLE, { search: 'json' })
    expect(result).toHaveLength(1)
    expect(result[0]?.name).toBe('JSON Transform')
  })

  it('combines category and search', () => {
    const result = catalogFilter(SAMPLE, {
      category: 'Destination',
      search: 's3',
    })
    expect(result).toHaveLength(1)
    expect(result[0]?.id).toBe('plet-c')
  })

  it('filters by active / inactive status', () => {
    expect(catalogFilter(SAMPLE, { status: 'Active' })).toHaveLength(1)
    expect(catalogFilter(SAMPLE, { status: 'Active' })[0]?.id).toBe('plet-b')
    expect(catalogFilter(SAMPLE, { status: 'Inactive' })).toHaveLength(2)
    expect(activePipelets(SAMPLE).map((p) => p.id)).toEqual(['plet-b'])
  })

  it('filters by scope and group', () => {
    expect(catalogFilter(SAMPLE, { scope: 'system' })).toHaveLength(2)
    expect(catalogFilter(SAMPLE, { scope: 'tenant' })).toHaveLength(1)
    expect(
      catalogFilter(SAMPLE, { scope: 'system', group: 'http' })[0]?.id,
    ).toBe('plet-a')
  })
})

describe('pipeletTaxonomy', () => {
  it('resolves path segments', () => {
    expect(resolvePathTaxonomy('source/http/plet-rest-source')).toEqual({
      category: 'Source',
      group: 'http',
    })
    expect(resolvePathTaxonomy('transformer/filter/plet-x')).toEqual({
      category: 'Processor',
      group: 'filter',
    })
  })

  it('enriches known system ids from PATHS', () => {
    const enriched = enrichPipeletTaxonomy({
      id: 'plet-rest-source',
      name: 'REST Source',
      category: 'Source',
      version: '1.0.0',
      runtime: 'Python',
      description: 'x',
    })
    expect(enriched.scope).toBe('system')
    expect(enriched.group).toBe('http')
  })

  it('overrides stale custom group from PATHS for system ids', () => {
    const enriched = enrichPipeletTaxonomy({
      id: 'plet-rest-source',
      name: 'REST Source',
      category: 'Source',
      version: '1.0.0',
      runtime: 'Python',
      description: 'x',
      scope: 'system',
      group: 'custom',
    })
    expect(enriched.group).toBe('http')
  })

  it('matches tree selection', () => {
    const item = SAMPLE[0]!
    expect(selectionMatches(item, { kind: 'all' })).toBe(true)
    expect(
      selectionMatches(item, { kind: 'scope', scope: 'system' }),
    ).toBe(true)
    expect(
      selectionMatches(item, {
        kind: 'group',
        scope: 'system',
        category: 'Source',
        group: 'http',
      }),
    ).toBe(true)
    expect(
      selectionMatches(item, {
        kind: 'group',
        scope: 'system',
        category: 'Source',
        group: 'database',
      }),
    ).toBe(false)
  })
})
