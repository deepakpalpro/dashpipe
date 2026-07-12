import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders } from '../../../test/renderWithProviders'
import { mockDb } from '../../../mocks/handlers'
import type { PipeletCatalogEntry } from '../../pipelets/catalogFilter'

vi.mock('./PipelineCanvas', () => ({
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

describe('PipelineBuilder.save', () => {
  it('saves three-stage graph via POST pipeline + PUT steps', async () => {
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

    await user.clear(screen.getByLabelText('Pipeline name'))
    await user.type(screen.getByLabelText('Pipeline name'), 'freshSave')

    await user.click(screen.getByRole('button', { name: /REST Source/ }))
    await user.click(screen.getByRole('button', { name: /JSON Transform/ }))
    await user.click(screen.getByRole('button', { name: /S3 Destination/ }))

    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      expect(mockDb.lastStepsPut).not.toBeNull()
    })
    expect(mockDb.lastStepsPut?.steps).toHaveLength(3)
    expect(mockDb.lastStepsPut?.steps.map((s) => s.pipelet_id)).toEqual([
      'plet-rest-source',
      'plet-json-transform',
      'plet-s3-destination',
    ])
    expect(mockDb.lastStepsPut?.steps.map((s) => s.step_order)).toEqual([1, 2, 3])

    await waitFor(() => {
      expect(screen.getByTestId('save-status')).toHaveTextContent(/Saved freshSave/)
    })
  })
})
