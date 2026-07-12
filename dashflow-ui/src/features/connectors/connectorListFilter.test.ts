import { describe, expect, it } from 'vitest'
import { filterConnectors, paginateItems } from './connectorListFilter'

const SAMPLE = [
  {
    id: 'conn-plet-rest-source',
    name: 'REST Source (plet-rest-source)',
    connectorTypeId: 'ct-rest',
    status: 'ACTIVE',
  },
  {
    id: 'conn-plet-webhook-source',
    name: 'Webhook Source (plet-webhook-source)',
    connectorTypeId: 'ct-event-listener',
    status: 'ACTIVE',
  },
  {
    id: 'conn-plet-s3-source',
    name: 'S3 Source (plet-s3-source)',
    connectorTypeId: 'ct-storage',
    status: 'INACTIVE',
  },
  {
    id: 'conn-orders',
    name: 'Orders API',
    connectorTypeId: 'ct-rest',
    status: 'ERROR',
  },
]

describe('filterConnectors', () => {
  it('filters by search on name and id', () => {
    expect(filterConnectors(SAMPLE, { search: 'webhook' })).toHaveLength(1)
    expect(filterConnectors(SAMPLE, { search: 'conn-orders' })[0]?.name).toBe(
      'Orders API',
    )
  })

  it('filters by type and status', () => {
    expect(
      filterConnectors(SAMPLE, { typeId: 'ct-rest', status: 'ACTIVE' }),
    ).toEqual([SAMPLE[0]])
    expect(filterConnectors(SAMPLE, { status: 'error' })).toEqual([SAMPLE[3]])
  })
})

describe('paginateItems', () => {
  it('pages results and clamps page', () => {
    const page0 = paginateItems(SAMPLE, 0, 2)
    expect(page0.pageItems).toHaveLength(2)
    expect(page0.totalPages).toBe(2)
    expect(page0.page).toBe(0)

    const pageHigh = paginateItems(SAMPLE, 99, 2)
    expect(pageHigh.page).toBe(1)
    expect(pageHigh.pageItems).toHaveLength(2)
  })
})
