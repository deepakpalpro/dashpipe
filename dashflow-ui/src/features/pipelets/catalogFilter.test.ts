import { describe, expect, it } from 'vitest'
import {
  activePipelets,
  catalogFilter,
  type PipeletCatalogEntry,
} from './catalogFilter'

const SAMPLE: PipeletCatalogEntry[] = [
  {
    id: 'plet-a',
    name: 'REST Source',
    category: 'Source',
    version: '1.0.0',
    runtime: 'Java',
    description: 'HTTP ingress',
    active: false,
  },
  {
    id: 'plet-b',
    name: 'JSON Transform',
    category: 'Processor',
    version: '1.0.0',
    runtime: 'Java',
    description: 'Map fields',
    active: true,
  },
  {
    id: 'plet-c',
    name: 'S3 Destination',
    category: 'Destination',
    version: '1.0.0',
    runtime: 'Java',
    description: 'Write objects',
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
})
