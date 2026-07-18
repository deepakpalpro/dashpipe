import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { PipelineExecutionSummary } from '../../../api/types'
import {
  EXECUTION_HISTORY_PAGE_SIZE,
  ExecutionHistoryPanel,
} from './ExecutionHistoryPanel'

function makeRuns(n: number): PipelineExecutionSummary[] {
  return Array.from({ length: n }, (_, i) => ({
    id: `exec-${i}`,
    pipeline_id: 'pipe-1',
    status: i === 0 ? 'RUNNING' : 'COMPLETED',
    trigger: 'manual',
    started_at: `2026-07-12T0${i}:00:00Z`,
    completed_at: i === 0 ? null : `2026-07-12T0${i}:01:00Z`,
    records_in: i,
    records_out: i,
    completeness_pct: 100,
  }))
}

describe('ExecutionHistoryPanel pagination', () => {
  it(`shows ${EXECUTION_HISTORY_PAGE_SIZE} rows per page`, async () => {
    const user = userEvent.setup()
    const onSelect = vi.fn()
    render(
      <MemoryRouter>
        <ExecutionHistoryPanel
          executions={makeRuns(7)}
          onSelect={onSelect}
          pipelineId="pipe-1"
        />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('execution-history-range')).toHaveTextContent(
      '1–3 of 7',
    )
    expect(screen.getByTestId('execution-history-page')).toHaveTextContent(
      'Page 1 / 3',
    )
    expect(screen.getAllByTestId(/execution-row-/)).toHaveLength(3)

    await user.click(screen.getByRole('button', { name: /Next page/i }))
    expect(screen.getByTestId('execution-history-range')).toHaveTextContent(
      '4–6 of 7',
    )
    expect(screen.getAllByTestId(/execution-row-/)).toHaveLength(3)

    await user.click(screen.getByRole('button', { name: /Next page/i }))
    expect(screen.getByTestId('execution-history-range')).toHaveTextContent(
      '7–7 of 7',
    )
    expect(screen.getAllByTestId(/execution-row-/)).toHaveLength(1)
  })

  it('jumps to the page containing the selected run', () => {
    render(
      <MemoryRouter>
        <ExecutionHistoryPanel
          executions={makeRuns(7)}
          selectedId="exec-5"
          onSelect={vi.fn()}
        />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('execution-history-page')).toHaveTextContent(
      'Page 2 / 3',
    )
    expect(screen.getByTestId('execution-row-exec-5')).toHaveAttribute(
      'data-selected',
      'true',
    )
  })
})
