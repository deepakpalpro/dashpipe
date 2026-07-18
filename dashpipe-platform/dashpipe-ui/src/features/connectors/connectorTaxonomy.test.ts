import { describe, expect, it } from 'vitest'
import {
  enrichConnector,
  pipeletIdFromConnector,
  connectorSelectionMatches,
  connectorGroupsFor,
} from './connectorTaxonomy'

describe('connectorTaxonomy', () => {
  it('resolves pipelet id from connector id and name', () => {
    expect(
      pipeletIdFromConnector({
        id: 'conn-plet-rest-source',
        name: 'REST Source',
      }),
    ).toBe('plet-rest-source')
    expect(
      pipeletIdFromConnector({
        id: 'conn-orders',
        name: 'Orders API (plet-rest-source)',
      }),
    ).toBe('plet-rest-source')
  })

  it('enriches connectors with pipelet kind and group', () => {
    const enriched = enrichConnector({
      id: 'conn-plet-rest-source',
      tenantId: 'T001',
      connectorTypeId: 'ct-rest',
      name: 'REST Source (plet-rest-source)',
      config: {},
      status: 'ACTIVE',
      lastTestedAt: null,
      createdAt: '2026-01-01T00:00:00Z',
    })
    expect(enriched.pipeletId).toBe('plet-rest-source')
    expect(enriched.category).toBe('Source')
    expect(enriched.group).toBe('http')
  })

  it('matches tree selection by category and group', () => {
    const item = enrichConnector({
      id: 'conn-plet-kafka-source',
      tenantId: 'T001',
      connectorTypeId: 'ct-message-bus',
      name: 'Kafka Source (plet-kafka-source)',
      config: {},
      status: 'ACTIVE',
      lastTestedAt: null,
      createdAt: '2026-01-01T00:00:00Z',
    })
    expect(
      connectorSelectionMatches(item, { kind: 'category', category: 'Source' }),
    ).toBe(true)
    expect(
      connectorSelectionMatches(item, {
        kind: 'group',
        category: 'Source',
        group: 'messaging',
      }),
    ).toBe(true)
    expect(
      connectorSelectionMatches(item, {
        kind: 'group',
        category: 'Source',
        group: 'http',
      }),
    ).toBe(false)
  })

  it('orders source groups canonically', () => {
    const items = [
      enrichConnector({
        id: 'conn-plet-rest-source',
        tenantId: 'T001',
        connectorTypeId: 'ct-rest',
        name: 'REST Source (plet-rest-source)',
        config: {},
        status: 'ACTIVE',
        lastTestedAt: null,
        createdAt: '2026-01-01T00:00:00Z',
      }),
      enrichConnector({
        id: 'conn-plet-jdbc-source',
        tenantId: 'T001',
        connectorTypeId: 'ct-db',
        name: 'JDBC Source (plet-jdbc-source)',
        config: {},
        status: 'ACTIVE',
        lastTestedAt: null,
        createdAt: '2026-01-01T00:00:00Z',
      }),
    ]
    expect(connectorGroupsFor(items, 'Source')).toEqual(['database', 'http'])
  })
})
