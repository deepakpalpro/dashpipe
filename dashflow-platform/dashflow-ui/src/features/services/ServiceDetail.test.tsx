import { describe, expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ServicesPage } from './ServicesPage'
import { renderWithProviders } from '../../test/renderWithProviders'

describe('ServiceDetail', () => {
  it('does not show raw secret from GET response', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ServicesPage />, { initialEntries: ['/services'] })

    await waitFor(() => {
      expect(screen.getByText('Primary Auth')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Primary Auth' }))

    await waitFor(() => {
      expect(screen.getByLabelText('Client secret')).toBeInTheDocument()
    })

    expect(screen.queryByText('raw-secret-must-not-leak')).not.toBeInTheDocument()
    expect(document.body.textContent).not.toContain('raw-secret-must-not-leak')
  })
})
