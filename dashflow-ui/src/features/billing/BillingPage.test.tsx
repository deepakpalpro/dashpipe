import { describe, expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../../test/renderWithProviders'
import { BillingPage } from './BillingPage'

describe('BillingPage', () => {
  it('shows monthly usage, credit, quota, and periods', async () => {
    renderWithProviders(<BillingPage />, { initialEntries: ['/billing'] })

    await waitFor(() => {
      expect(screen.getByTestId('credit-balance')).toHaveTextContent('$94.4300')
    })
    expect(screen.getByTestId('mtd-cost')).toHaveTextContent('$5.5700')
    expect(screen.getByTestId('quota-decision')).toHaveTextContent('ALLOW')
    const dims = screen.getByLabelText('Usage by dimension')
    expect(dims).toHaveTextContent('platform.pipeline_runs')
    expect(dims).toHaveTextContent('data.records_processed')
    expect(screen.getByLabelText('Billing periods')).toHaveTextContent(/open/i)
    expect(screen.getByRole('heading', { name: 'Recent usage events' })).toBeInTheDocument()
  })
})
