export type StepCategory = 'Source' | 'Processor' | 'Destination'

export type PipelineStepNodeData = {
  pipeletId: string
  name: string
  category: StepCategory
  connectorIds: string[]
  serviceIds: string[]
  /** @deprecated Prefer executionConfig; kept in sync for save payload. */
  config: Record<string, unknown>
  deploymentConfig: Record<string, unknown>
  executionConfig: Record<string, unknown>
}

export type PipelineGraphNode = {
  id: string
  position: { x: number; y: number }
  data: PipelineStepNodeData
}

export type PipelineGraphEdge = {
  id: string
  source: string
  target: string
  label?: string
}

export type PipelineGraphState = {
  nodes: PipelineGraphNode[]
  edges: PipelineGraphEdge[]
  selectedNodeId: string | null
  pipelineName: string
  deploymentConfig: Record<string, unknown>
  executionConfig: Record<string, unknown>
}

export type PipelineGraphAction =
  | {
      type: 'ADD_NODE'
      node: PipelineGraphNode
    }
  | {
      type: 'CONNECT'
      source: string
      target: string
      label?: string
    }
  | {
      type: 'UPDATE_STEP'
      nodeId: string
      patch: Partial<PipelineStepNodeData>
    }
  | { type: 'REMOVE_NODE'; nodeId: string }
  | { type: 'SELECT_NODE'; nodeId: string | null }
  | { type: 'SET_PIPELINE_NAME'; name: string }
  | {
      type: 'SET_DEPLOYMENT_CONFIG'
      deploymentConfig: Record<string, unknown>
    }
  | {
      type: 'SET_EXECUTION_CONFIG'
      executionConfig: Record<string, unknown>
    }
  | { type: 'RESET'; state?: PipelineGraphState }

export const initialPipelineGraph: PipelineGraphState = {
  nodes: [],
  edges: [],
  selectedNodeId: null,
  pipelineName: 'Untitled pipeline',
  deploymentConfig: {},
  executionConfig: {},
}

export function pipelineGraphReducer(
  state: PipelineGraphState,
  action: PipelineGraphAction,
): PipelineGraphState {
  switch (action.type) {
    case 'ADD_NODE':
      return {
        ...state,
        nodes: [...state.nodes, action.node],
        selectedNodeId: action.node.id,
      }
    case 'CONNECT': {
      if (action.source === action.target) {
        return state
      }
      const exists = state.edges.some(
        (e) => e.source === action.source && e.target === action.target,
      )
      if (exists) {
        return state
      }
      const edge: PipelineGraphEdge = {
        id: `e-${action.source}-${action.target}`,
        source: action.source,
        target: action.target,
        label: action.label ?? `q-${action.source}-to-${action.target}`,
      }
      return { ...state, edges: [...state.edges, edge] }
    }
    case 'UPDATE_STEP': {
      const patch = { ...action.patch }
      if (patch.executionConfig && !patch.config) {
        patch.config = patch.executionConfig
      }
      if (patch.config && !patch.executionConfig) {
        patch.executionConfig = patch.config
      }
      return {
        ...state,
        nodes: state.nodes.map((n) =>
          n.id === action.nodeId
            ? { ...n, data: { ...n.data, ...patch } }
            : n,
        ),
      }
    }
    case 'REMOVE_NODE':
      return {
        ...state,
        nodes: state.nodes.filter((n) => n.id !== action.nodeId),
        edges: state.edges.filter(
          (e) => e.source !== action.nodeId && e.target !== action.nodeId,
        ),
        selectedNodeId:
          state.selectedNodeId === action.nodeId ? null : state.selectedNodeId,
      }
    case 'SELECT_NODE':
      return { ...state, selectedNodeId: action.nodeId }
    case 'SET_PIPELINE_NAME':
      return { ...state, pipelineName: action.name }
    case 'SET_DEPLOYMENT_CONFIG':
      return { ...state, deploymentConfig: action.deploymentConfig }
    case 'SET_EXECUTION_CONFIG':
      return { ...state, executionConfig: action.executionConfig }
    case 'RESET':
      return action.state ?? initialPipelineGraph
    default:
      return state
  }
}

/** Topological order by edges; falls back to insertion order for isolates. */
export function orderedNodeIds(state: PipelineGraphState): string[] {
  const ids = state.nodes.map((n) => n.id)
  const incoming = new Map(ids.map((id) => [id, 0]))
  const adj = new Map(ids.map((id) => [id, [] as string[]]))
  for (const e of state.edges) {
    if (!incoming.has(e.target) || !adj.has(e.source)) {
      continue
    }
    incoming.set(e.target, (incoming.get(e.target) ?? 0) + 1)
    adj.get(e.source)!.push(e.target)
  }
  const queue = ids.filter((id) => (incoming.get(id) ?? 0) === 0)
  const ordered: string[] = []
  while (queue.length) {
    const id = queue.shift()!
    ordered.push(id)
    for (const next of adj.get(id) ?? []) {
      const nextIn = (incoming.get(next) ?? 1) - 1
      incoming.set(next, nextIn)
      if (nextIn === 0) {
        queue.push(next)
      }
    }
  }
  for (const id of ids) {
    if (!ordered.includes(id)) {
      ordered.push(id)
    }
  }
  return ordered
}

export type PipelineStepPayload = {
  pipelet_id: string
  step_order: number
  config: Record<string, unknown>
  deployment_config: Record<string, unknown>
  execution_config: Record<string, unknown>
  connector_ids: string[]
  service_ids: string[]
  input_queue: string | null
  output_queue: string | null
}

export type ReplaceStepsPayload = {
  steps: PipelineStepPayload[]
}

export function graphToStepsPayload(
  state: PipelineGraphState,
): ReplaceStepsPayload {
  const order = orderedNodeIds(state)
  const byId = new Map(state.nodes.map((n) => [n.id, n]))
  const steps: PipelineStepPayload[] = order.map((id, index) => {
    const node = byId.get(id)!
    const inbound = state.edges.find((e) => e.target === id)
    const outbound = state.edges.find((e) => e.source === id)
    const execution = node.data.executionConfig ?? node.data.config ?? {}
    const deployment = node.data.deploymentConfig ?? {}
    return {
      pipelet_id: node.data.pipeletId,
      step_order: index + 1,
      config: execution,
      deployment_config: deployment,
      execution_config: execution,
      connector_ids: node.data.connectorIds,
      service_ids: node.data.serviceIds,
      input_queue: inbound?.label ?? null,
      output_queue: outbound?.label ?? null,
    }
  })
  return { steps }
}

export type PipelineStepLike = {
  pipelet_id: string
  step_order: number
  config?: Record<string, unknown> | null
  deployment_config?: Record<string, unknown> | null
  execution_config?: Record<string, unknown> | null
  connector_ids?: string[]
  service_ids?: string[]
  input_queue?: string | null
  output_queue?: string | null
}

/** Rebuild a linear canvas graph from API steps (ordered by step_order). */
export function stepsToGraph(
  pipelineName: string,
  steps: PipelineStepLike[],
  resolveName: (pipeletId: string) => {
    name: string
    category: StepCategory
    deploymentConfiguration?: Record<string, unknown>
    executionConfiguration?: Record<string, unknown>
  },
  deploymentConfig: Record<string, unknown> = {},
  executionConfig: Record<string, unknown> = {},
): PipelineGraphState {
  const ordered = [...steps].sort((a, b) => a.step_order - b.step_order)
  const nodes: PipelineGraphNode[] = ordered.map((step, index) => {
    const meta = resolveName(step.pipelet_id)
    const execution =
      (step.execution_config as Record<string, unknown> | null) ??
      (step.config as Record<string, unknown> | null) ??
      {}
    const deployment =
      (step.deployment_config as Record<string, unknown> | null) ?? {}
    return {
      id: `n${index + 1}`,
      position: { x: 40 + index * 220, y: 120 },
      data: {
        pipeletId: step.pipelet_id,
        name: meta.name,
        category: meta.category,
        connectorIds: step.connector_ids ?? [],
        serviceIds: step.service_ids ?? [],
        config: execution,
        deploymentConfig: deployment,
        executionConfig: execution,
      },
    }
  })
  const edges: PipelineGraphEdge[] = []
  for (let i = 0; i < nodes.length - 1; i++) {
    const source = nodes[i]
    const target = nodes[i + 1]
    const fromStep = ordered[i]
    edges.push({
      id: `e-${source.id}-${target.id}`,
      source: source.id,
      target: target.id,
      label: fromStep.output_queue ?? `q-stage-${i + 1}`,
    })
  }
  return {
    nodes,
    edges,
    selectedNodeId: nodes[0]?.id ?? null,
    pipelineName,
    deploymentConfig: { ...deploymentConfig },
    executionConfig: { ...executionConfig },
  }
}
