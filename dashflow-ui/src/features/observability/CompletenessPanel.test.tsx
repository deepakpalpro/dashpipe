import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { CompletenessPanel } from './CompletenessPanel'

describe('CompletenessPanel', () => {
  it('renders completeness percent from mock series', () => {
    render(
      <CompletenessPanel
        data={{
          pipeline_id: 'pipe-1',
          tenant_id: 'T001',
          execution_id: 'exec-1',
          records_in: 100,
          records_out: 98,
          completeness_pct: 98,
          completeness_ratio: 0.98,
        }}
      />,
    )
    expect(screen.getByTestId('completeness-pct')).toHaveTextContent('98%')
  })
})
