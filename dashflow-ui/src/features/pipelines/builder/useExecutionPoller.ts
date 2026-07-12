import { useEffect, useRef, useState } from 'react'
import { getPipelineExecution } from '../../../api/resources'
import type { PipelineExecutionDetail } from '../../../api/types'
import { isTerminalExecutionStatus } from './executionOverlayReducer'

type Options = {
  tenantId: string
  pipelineId: string | null
  executionId: string | null
  enabled?: boolean
  intervalMs?: number
  maxAttempts?: number
  /** Bump to restart polling (e.g. after force-stop). */
  pollKey?: number
}

export function useExecutionPoller({
  tenantId,
  pipelineId,
  executionId,
  enabled = true,
  intervalMs = 200,
  maxAttempts = 20,
  pollKey = 0,
}: Options) {
  const [execution, setExecution] = useState<PipelineExecutionDetail | null>(
    null,
  )
  const [error, setError] = useState<string | null>(null)
  const attempts = useRef(0)

  useEffect(() => {
    // Drop previous run immediately so UI does not show mismatched status/logs.
    setExecution(null)
    setError(null)
    attempts.current = 0

    if (!enabled || !pipelineId || !executionId) {
      return
    }
    let cancelled = false
    let timer: ReturnType<typeof setTimeout> | undefined
    const selectedId = executionId

    async function tick() {
      try {
        const result = await getPipelineExecution(
          tenantId,
          pipelineId!,
          selectedId,
        )
        if (cancelled) {
          return
        }
        setExecution(result)
        attempts.current += 1
        if (
          isTerminalExecutionStatus(result.status) ||
          attempts.current >= maxAttempts
        ) {
          return
        }
        timer = setTimeout(() => {
          void tick()
        }, intervalMs)
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Poll failed')
        }
      }
    }

    void tick()
    return () => {
      cancelled = true
      if (timer) {
        clearTimeout(timer)
      }
    }
  }, [
    tenantId,
    pipelineId,
    executionId,
    enabled,
    intervalMs,
    maxAttempts,
    pollKey,
  ])

  return { execution, error }
}
