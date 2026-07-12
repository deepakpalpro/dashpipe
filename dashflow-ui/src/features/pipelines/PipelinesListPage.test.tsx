import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { AppShell } from '../../app/AppShell'
import { mockDb } from '../../mocks/handlers'
import { renderWithProviders } from '../../test/renderWithProviders'
import { PipelinesListPage } from './PipelinesListPage'

vi.mock('./builder/PipelineCanvas', () => ({
  PipelineCanvas: ({
    nodes,
  }: {
    nodes: { id: string; data: { name: string } }[]
  }) => (
    <div aria-label="Pipeline canvas">
      {nodes.map((n) => (
        <div key={n.id}>{n.data.name}</div>
      ))}
    </div>
  ),
}))

import { PipelineBuilderPage } from './builder/PipelineBuilderPage'

describe('PipelinesListPage', () => {
  it('lists saved pipelines with edit and delete actions', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <Routes>
        <Route path="/pipelines" element={<PipelinesListPage />} />
        <Route path="/pipelines/:pipelineId" element={<div>builder</div>} />
      </Routes>,
      { initialEntries: ['/pipelines'] },
    )

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /threeStage/ }),
      ).toBeInTheDocument()
    })
    expect(screen.getByRole('button', { name: /draftOnly/ })).toBeInTheDocument()

    const detail = screen.getByTestId('pipeline-detail')
    expect(
      within(detail).getByRole('heading', { name: 'threeStage' }),
    ).toBeInTheDocument()
    expect(within(detail).getByText('3')).toBeInTheDocument()

    const steps = within(detail).getByTestId('pipeline-steps-detail')
    expect(within(steps).getByText('REST Source')).toBeInTheDocument()
    expect(within(steps).getByText('JSON Transform')).toBeInTheDocument()
    expect(within(steps).getByText('S3 Destination')).toBeInTheDocument()
    expect(within(steps).getByText(/mode/)).toBeInTheDocument()
    expect(within(steps).getByText(/flatten/)).toBeInTheDocument()
    expect(within(steps).getByText(/conn-plet-rest-source/)).toBeInTheDocument()
    expect(within(steps).getByText(/svc-auth-oauth/)).toBeInTheDocument()

    expect(
      screen.getByRole('link', { name: 'Open in builder' }),
    ).toHaveAttribute('href', '/pipelines/pipe-demo')
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute(
      'href',
      '/pipelines/pipe-demo',
    )
    expect(screen.getByRole('link', { name: 'Observability' })).toHaveAttribute(
      'href',
      '/observability?pipelineId=pipe-demo',
    )

    vi.spyOn(window, 'confirm').mockReturnValue(true)
    await user.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => {
      expect(screen.getByTestId('pipeline-list-status')).toHaveTextContent(
        /Archived threeStage/,
      )
    })
    expect(mockDb.pipelines.find((p) => p.id === 'pipe-demo')?.status).toBe(
      'ARCHIVED',
    )
    await waitFor(() => {
      expect(
        screen.queryByRole('button', { name: /threeStage/ }),
      ).not.toBeInTheDocument()
    })
  })

  it('navigates to new builder from New', async () => {
    const user = userEvent.setup()
    renderWithProviders(<AppShell />, { initialEntries: ['/pipelines'] })

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: 'Pipelines' }),
      ).toBeInTheDocument()
    })
    await user.click(screen.getByRole('button', { name: 'New' }))
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: 'New pipeline' }),
      ).toBeInTheDocument()
    })
  })
})

describe('PipelineBuilder load', () => {
  it('hydrates canvas from GET pipeline', async () => {
    renderWithProviders(
      <Routes>
        <Route path="/pipelines/:pipelineId" element={<PipelineBuilderPage />} />
      </Routes>,
      { initialEntries: ['/pipelines/pipe-demo'] },
    )

    await waitFor(() => {
      expect(screen.getByLabelText('Pipeline name')).toHaveValue('threeStage')
    })
    const canvas = screen.getByLabelText('Pipeline canvas')
    expect(within(canvas).getByText('REST Source')).toBeInTheDocument()
    expect(within(canvas).getByText('JSON Transform')).toBeInTheDocument()
    expect(within(canvas).getByText('S3 Destination')).toBeInTheDocument()
  })
})
