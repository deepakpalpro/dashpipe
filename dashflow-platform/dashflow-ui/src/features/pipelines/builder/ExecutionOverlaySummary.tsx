import type { OverlayNodeState } from './executionOverlayReducer'

type Props = {
  byNodeId: Record<string, OverlayNodeState>
  nodeIds: string[]
}

export function ExecutionOverlaySummary({ byNodeId, nodeIds }: Props) {
  if (nodeIds.length === 0) {
    return null
  }
  return (
    <ul className="overlay-summary" aria-label="Execution overlay">
      {nodeIds.map((id) => {
        const state = byNodeId[id] ?? 'idle'
        return (
          <li key={id} data-testid={`overlay-${id}`} data-state={state}>
            <span className={`overlay-dot ${state}`} />
            {id}: {state}
          </li>
        )
      })}
    </ul>
  )
}
