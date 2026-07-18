import { beforeEach, describe, expect, it } from 'vitest'
import { getPipeletCatalog } from './fixture'
import {
  activatePipelets,
  clearActivatedPipelets,
  pipeletIdsFromBundleSteps,
} from './pipeletActivation'

describe('pipeletActivation', () => {
  beforeEach(() => {
    clearActivatedPipelets()
  })

  it('extracts distinct pipelet ids from bundle steps', () => {
    expect(
      pipeletIdsFromBundleSteps([
        { pipelet_id: 'plet-rest-source' },
        { pipelet_id: 'plet-python-filter' },
        { pipelet_id: 'plet-rest-source' },
        { pipelet_id: '  ' },
        {},
      ]),
    ).toEqual(['plet-rest-source', 'plet-python-filter'])
  })

  it('activates referenced pipelets in the catalog for the session', () => {
    const before = getPipeletCatalog().find((p) => p.id === 'plet-rest-source')
    expect(before?.active).not.toBe(true)

    const newly = activatePipelets([
      'plet-rest-source',
      'plet-python-filter',
      'plet-webhook-destination',
    ])
    expect(newly).toEqual([
      'plet-rest-source',
      'plet-python-filter',
      'plet-webhook-destination',
    ])

    const catalog = getPipeletCatalog()
    for (const id of newly) {
      expect(catalog.find((p) => p.id === id)?.active).toBe(true)
    }

    expect(activatePipelets(['plet-rest-source'])).toEqual([])
  })
})
