import { describe, expect, it } from 'vitest'
import {
  graphToStepsPayload,
  initialPipelineGraph,
  pipelineGraphReducer,
  type PipelineGraphNode,
} from './pipelineGraphReducer'

function node(
  id: string,
  pipeletId: string,
  category: 'Source' | 'Processor' | 'Destination',
  x: number,
): PipelineGraphNode {
  return {
    id,
    position: { x, y: 0 },
    data: {
      pipeletId,
      name: pipeletId,
      category,
      connectorIds: [],
      serviceIds: [],
      config: {},
    },
  }
}

describe('pipelineGraphReducer', () => {
  it('adds three nodes and two connections', () => {
    let state = initialPipelineGraph
    state = pipelineGraphReducer(state, {
      type: 'ADD_NODE',
      node: node('n1', 'plet-rest-source', 'Source', 0),
    })
    state = pipelineGraphReducer(state, {
      type: 'ADD_NODE',
      node: node('n2', 'plet-json-transform', 'Processor', 200),
    })
    state = pipelineGraphReducer(state, {
      type: 'ADD_NODE',
      node: node('n3', 'plet-s3-destination', 'Destination', 400),
    })
    state = pipelineGraphReducer(state, {
      type: 'CONNECT',
      source: 'n1',
      target: 'n2',
      label: 'q-stage-1',
    })
    state = pipelineGraphReducer(state, {
      type: 'CONNECT',
      source: 'n2',
      target: 'n3',
      label: 'q-stage-2',
    })

    expect(state.nodes).toHaveLength(3)
    expect(state.edges).toHaveLength(2)
  })

  it('removes a node and its connected edges', () => {
    let state = initialPipelineGraph
    state = pipelineGraphReducer(state, {
      type: 'ADD_NODE',
      node: node('n1', 'plet-rest-source', 'Source', 0),
    })
    state = pipelineGraphReducer(state, {
      type: 'ADD_NODE',
      node: node('n2', 'plet-json-transform', 'Processor', 200),
    })
    state = pipelineGraphReducer(state, {
      type: 'ADD_NODE',
      node: node('n3', 'plet-s3-destination', 'Destination', 400),
    })
    state = pipelineGraphReducer(state, {
      type: 'CONNECT',
      source: 'n1',
      target: 'n2',
    })
    state = pipelineGraphReducer(state, {
      type: 'CONNECT',
      source: 'n2',
      target: 'n3',
    })
    state = pipelineGraphReducer(state, {
      type: 'SELECT_NODE',
      nodeId: 'n2',
    })
    state = pipelineGraphReducer(state, { type: 'REMOVE_NODE', nodeId: 'n2' })

    expect(state.nodes.map((n) => n.id)).toEqual(['n1', 'n3'])
    expect(state.edges).toHaveLength(0)
    expect(state.selectedNodeId).toBeNull()
  })

  it('serializes a three-stage graph to ordered steps payload', () => {
    let state = initialPipelineGraph
    state = pipelineGraphReducer(state, {
      type: 'ADD_NODE',
      node: node('n1', 'plet-rest-source', 'Source', 0),
    })
    state = pipelineGraphReducer(state, {
      type: 'ADD_NODE',
      node: node('n2', 'plet-json-transform', 'Processor', 200),
    })
    state = pipelineGraphReducer(state, {
      type: 'ADD_NODE',
      node: node('n3', 'plet-s3-destination', 'Destination', 400),
    })
    state = pipelineGraphReducer(state, {
      type: 'UPDATE_STEP',
      nodeId: 'n1',
      patch: { connectorIds: ['conn-1'] },
    })
    state = pipelineGraphReducer(state, {
      type: 'UPDATE_STEP',
      nodeId: 'n3',
      patch: { serviceIds: ['svc-1'] },
    })
    state = pipelineGraphReducer(state, {
      type: 'CONNECT',
      source: 'n1',
      target: 'n2',
      label: 'q-stage-1',
    })
    state = pipelineGraphReducer(state, {
      type: 'CONNECT',
      source: 'n2',
      target: 'n3',
      label: 'q-stage-2',
    })

    const payload = graphToStepsPayload(state)
    expect(payload.steps).toHaveLength(3)
    expect(payload.steps.map((s) => s.step_order)).toEqual([1, 2, 3])
    expect(payload.steps.map((s) => s.pipelet_id)).toEqual([
      'plet-rest-source',
      'plet-json-transform',
      'plet-s3-destination',
    ])
    expect(payload.steps[0]?.connector_ids).toEqual(['conn-1'])
    expect(payload.steps[2]?.service_ids).toEqual(['svc-1'])
    expect(payload.steps[0]?.output_queue).toBe('q-stage-1')
    expect(payload.steps[1]?.input_queue).toBe('q-stage-1')
  })
})
