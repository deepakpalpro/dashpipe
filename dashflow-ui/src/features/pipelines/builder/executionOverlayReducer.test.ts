import { describe, expect, it } from 'vitest'
import {
  executionOverlayReducer,
  initialOverlayState,
} from './executionOverlayReducer'

describe('executionOverlayReducer', () => {
  it('maps RUNNING/COMPLETED/FAILED step statuses to node states', () => {
    const state = executionOverlayReducer(initialOverlayState, {
      type: 'APPLY_STEPS',
      nodeIdsInOrder: ['n1', 'n2', 'n3'],
      executionStatus: 'RUNNING',
      steps: [
        { step_order: 1, status: 'COMPLETED' },
        { step_order: 2, status: 'RUNNING' },
        { step_order: 3, status: 'PENDING' },
      ],
    })
    expect(state.byNodeId).toEqual({
      n1: 'completed',
      n2: 'running',
      n3: 'pending',
    })
  })

  it('marks failed step as failed', () => {
    const state = executionOverlayReducer(initialOverlayState, {
      type: 'APPLY_STEPS',
      nodeIdsInOrder: ['n1', 'n2'],
      executionStatus: 'FAILED',
      steps: [
        { step_order: 1, status: 'COMPLETED' },
        { step_order: 2, status: 'FAILED' },
      ],
    })
    expect(state.byNodeId.n2).toBe('failed')
  })
})
