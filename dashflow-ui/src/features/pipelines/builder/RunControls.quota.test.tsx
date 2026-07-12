import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders } from '../../../test/renderWithProviders'
import { mockDb } from '../../../mocks/handlers'
import type { PipeletCatalogEntry } from '../../pipelets/catalogFilter'

vi.mock('./PipelineCanvas', () => ({
  PipelineCanvas: () => <div aria-label="Pipeline canvas" />,
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
]

describe('RunControls.quota', () => {
  it('shows quota message when run returns 402', async () => {
    mockDb.blockRunsWith402 = true
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
    await user.click(screen.getByRole('button', { name: 'Run' }))

    await waitFor(() => {
      expect(screen.getByTestId('quota-alert')).toBeInTheDocument()
    })
    expect(screen.getByTestId('quota-alert')).toHaveTextContent(
      /no credit|Credit balance/i,
    )
  })
})
