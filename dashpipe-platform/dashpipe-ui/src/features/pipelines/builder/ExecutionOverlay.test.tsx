import { describe, expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { vi } from 'vitest'
import { renderWithProviders } from '../../../test/renderWithProviders'
import type { PipeletCatalogEntry } from '../../pipelets/catalogFilter'

vi.mock('./PipelineCanvas', () => ({
  PipelineCanvas: ({
    nodes,
    overlayByNodeId = {},
  }: {
    nodes: { id: string; data: { name: string } }[]
    overlayByNodeId?: Record<string, string>
  }) => (
    <div aria-label="Pipeline canvas">
      {nodes.map((n) => (
        <div key={n.id} data-testid={`canvas-${n.id}`} data-overlay={overlayByNodeId[n.id] ?? 'idle'}>
          {n.data.name}
        </div>
      ))}
    </div>
  ),
}))

import { PipelineBuilderPage } from './PipelineBuilderPage'

const CATALOG: PipeletCatalogEntry[] = [
  {
    id: 'plet-rest-source',
    name: 'REST Source',
    category: 'Source',
    version: '1.0.0',
    runtime: 'Java',
    description: 'src',
    active: true,
  },
  {
    id: 'plet-json-transform',
    name: 'JSON Transform',
    category: 'Processor',
    version: '1.0.0',
    runtime: 'Java',
    description: 'proc',
    active: true,
  },
  {
    id: 'plet-s3-destination',
    name: 'S3 Destination',
    category: 'Destination',
    version: '1.0.0',
    runtime: 'Java',
    description: 'dst',
    active: true,
  },
]

describe('ExecutionOverlay', () => {
  it('polls until completed and shows completed overlay states', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <Routes>
        <Route path="/pipelines/new" element={<PipelineBuilderPage catalog={CATALOG} />} />
        <Route
          path="/pipelines/:pipelineId"
          element={<PipelineBuilderPage catalog={CATALOG} />}
        />
      </Routes>,
      { initialEntries: ['/pipelines/new'] },
    )

    await user.click(screen.getByRole('button', { name: /REST Source/ }))
    await user.click(screen.getByRole('button', { name: /JSON Transform/ }))
    await user.click(screen.getByRole('button', { name: /S3 Destination/ }))
    await user.click(screen.getByRole('button', { name: 'Run' }))

    await waitFor(
      () => {
        expect(screen.getByTestId('overlay-n1')).toHaveAttribute(
          'data-state',
          'completed',
        )
        expect(screen.getByTestId('overlay-n2')).toHaveAttribute(
          'data-state',
          'completed',
        )
        expect(screen.getByTestId('overlay-n3')).toHaveAttribute(
          'data-state',
          'completed',
        )
      },
      { timeout: 8000 },
    )

    expect(
      await screen.findByRole('region', { name: 'Execution history' }),
    ).toBeInTheDocument()
  }, 15000)
})
