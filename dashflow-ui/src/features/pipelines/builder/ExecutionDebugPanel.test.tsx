import { useState } from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../../mocks/server'
import { renderWithProviders } from '../../../test/renderWithProviders'
import { ExecutionDebugPanel } from './ExecutionDebugPanel'

describe('ExecutionDebugPanel', () => {
  it('shows empty state without execution', () => {
    renderWithProviders(<ExecutionDebugPanel executionId={null} />)
    expect(
      screen.getByText(/Select a run from history/i),
    ).toBeInTheDocument()
  })

  it('renders logs and kubectl hints for a selected run', async () => {
    server.use(
      http.get('/api/v1/pipelines/:id/executions/:executionId', () =>
        HttpResponse.json({
          id: 'exec-1',
          pipeline_id: 'pipe-1',
          status: 'RUNNING',
          records_in: 0,
          records_out: 0,
          started_at: '2026-07-12T00:00:00Z',
        }),
      ),
      http.get('/api/v1/observability/executions/:execId/logs', () =>
        HttpResponse.json({
          execution_id: 'exec-1',
          tenant_id: 'T001',
          pipeline_id: 'pipe-1',
          logs: [
            {
              '@timestamp': '2026-07-12T00:00:00Z',
              level: 'INFO',
              pipelet_id: 'plet-s3-source',
              message: 'fetched object',
              records_out: 3,
            },
          ],
        }),
      ),
    )

    renderWithProviders(
      <ExecutionDebugPanel
        executionId="exec-1"
        pipelineId="pipe-1"
        execution={{
          id: 'exec-1',
          pipeline_id: 'pipe-1',
          status: 'RUNNING',
          records_in: 0,
          records_out: 0,
          started_at: '2026-07-12T00:00:00Z',
        }}
      />,
    )

    expect(
      screen.getByRole('heading', { name: /Debug \/ logs/i }),
    ).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByTestId('execution-log-view')).toHaveTextContent(
        'fetched object',
      )
    })
    expect(screen.getByText(/kubectl get jobs,pods/)).toBeInTheDocument()
  })

  it('swaps logs when executionId changes', async () => {
    const user = userEvent.setup()
    server.use(
      http.get('/api/v1/pipelines/:id/executions/:executionId', ({ params }) =>
        HttpResponse.json({
          id: String(params.executionId),
          pipeline_id: 'pipe-1',
          status: 'COMPLETED',
          records_in: 1,
          records_out: 1,
          started_at: '2026-07-12T00:00:00Z',
          completed_at: '2026-07-12T00:01:00Z',
        }),
      ),
      http.get('/api/v1/observability/executions/:execId/logs', ({ params }) =>
        HttpResponse.json({
          execution_id: String(params.execId),
          tenant_id: 'T001',
          pipeline_id: 'pipe-1',
          logs: [
            {
              '@timestamp': '2026-07-12T00:00:00Z',
              level: 'INFO',
              message: `log-for-${params.execId}`,
            },
          ],
        }),
      ),
    )

    function Switcher() {
      const [id, setId] = useState('exec-a')
      return (
        <div>
          <button type="button" onClick={() => setId('exec-b')}>
            Select B
          </button>
          <ExecutionDebugPanel
            key={id}
            executionId={id}
            pipelineId="pipe-1"
            hideHeader
          />
        </div>
      )
    }

    renderWithProviders(<Switcher />)

    await waitFor(() => {
      expect(screen.getByTestId('execution-log-view')).toHaveTextContent(
        'log-for-exec-a',
      )
    })

    await user.click(screen.getByRole('button', { name: /Select B/i }))

    await waitFor(() => {
      expect(screen.getByTestId('execution-log-view')).toHaveTextContent(
        'log-for-exec-b',
      )
    })
    expect(screen.getByTestId('execution-log-view')).not.toHaveTextContent(
      'log-for-exec-a',
    )
  })

  it('flags stub-like completed runs', () => {
    renderWithProviders(
      <ExecutionDebugPanel
        executionId="exec-stub"
        execution={{
          id: 'exec-stub',
          pipeline_id: 'pipe-1',
          status: 'COMPLETED',
          records_in: 0,
          records_out: 0,
          started_at: '2026-07-12T00:00:00Z',
          completed_at: '2026-07-12T00:00:02Z',
        }}
      />,
    )
    expect(screen.getByText(/stub mode/i)).toBeInTheDocument()
  })
})
