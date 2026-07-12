export type OverlayNodeState =
  | 'pending'
  | 'running'
  | 'completed'
  | 'failed'
  | 'idle'

export type ExecutionStepStatus = {
  step_order: number
  node_id?: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
}

export type OverlayState = {
  byNodeId: Record<string, OverlayNodeState>
  executionStatus: string | null
}

export type OverlayAction =
  | { type: 'RESET' }
  | {
      type: 'APPLY_STEPS'
      nodeIdsInOrder: string[]
      steps: ExecutionStepStatus[]
      executionStatus: string
    }
  | {
      type: 'APPLY_OVERALL'
      nodeIdsInOrder: string[]
      executionStatus: string
    }

export const initialOverlayState: OverlayState = {
  byNodeId: {},
  executionStatus: null,
}

function mapStepStatus(
  status: ExecutionStepStatus['status'],
): OverlayNodeState {
  switch (status) {
    case 'RUNNING':
      return 'running'
    case 'COMPLETED':
      return 'completed'
    case 'FAILED':
      return 'failed'
    default:
      return 'pending'
  }
}

export function executionOverlayReducer(
  state: OverlayState,
  action: OverlayAction,
): OverlayState {
  switch (action.type) {
    case 'RESET':
      return initialOverlayState
    case 'APPLY_STEPS': {
      const byNodeId: Record<string, OverlayNodeState> = {}
      for (const id of action.nodeIdsInOrder) {
        byNodeId[id] = 'pending'
      }
      for (const step of action.steps) {
        const nodeId =
          step.node_id ?? action.nodeIdsInOrder[step.step_order - 1]
        if (nodeId) {
          byNodeId[nodeId] = mapStepStatus(step.status)
        }
      }
      return { byNodeId, executionStatus: action.executionStatus }
    }
    case 'APPLY_OVERALL': {
      const byNodeId: Record<string, OverlayNodeState> = {}
      const status = action.executionStatus.toUpperCase()
      for (let i = 0; i < action.nodeIdsInOrder.length; i++) {
        const id = action.nodeIdsInOrder[i]!
        if (status === 'COMPLETED' || status === 'SUCCEEDED') {
          byNodeId[id] = 'completed'
        } else if (status === 'FAILED') {
          byNodeId[id] =
            i === action.nodeIdsInOrder.length - 1 ? 'failed' : 'completed'
        } else if (status === 'RUNNING') {
          byNodeId[id] = i === 0 ? 'running' : 'pending'
        } else {
          byNodeId[id] = 'pending'
        }
      }
      return { byNodeId, executionStatus: action.executionStatus }
    }
    default:
      return state
  }
}

export function isTerminalExecutionStatus(status: string | null | undefined): boolean {
  if (!status) {
    return false
  }
  const s = status.toUpperCase()
  return s === 'COMPLETED' || s === 'SUCCEEDED' || s === 'FAILED' || s === 'CANCELLED'
}
