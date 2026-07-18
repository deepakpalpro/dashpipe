import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { LatencyPanel } from './LatencyPanel'

describe('LatencyPanel', () => {
  it('shows p95 label from mock latency summary', () => {
    render(
      <LatencyPanel
        data={{
          pipeline_id: 'pipe-1',
          tenant_id: 'T001',
          sample_count: 12,
          mean_ms: 40,
          max_ms: 120,
          p50_ms: 35,
          p95_ms: 95,
          p99_ms: 110,
        }}
      />,
    )
    expect(screen.getByTestId('latency-p95')).toHaveTextContent('95 ms')
    expect(screen.getByText('p95')).toBeInTheDocument()
  })
})
