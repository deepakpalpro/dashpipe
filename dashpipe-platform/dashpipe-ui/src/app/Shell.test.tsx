import { describe, expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AppShell } from '../app/AppShell'
import { renderWithProviders } from '../test/renderWithProviders'

describe('AppShell', () => {
  it('renders Level-1 primary nav links', () => {
    renderWithProviders(<AppShell />, { initialEntries: ['/pipelets'] })

    expect(screen.getByRole('navigation', { name: 'Primary' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Pipelets' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Pipelines' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Connectors' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Services' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tenants' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Observability' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Billing' })).toBeInTheDocument()
  })

  it('highlights the active route and navigates to stub pages', async () => {
    const user = userEvent.setup()
    renderWithProviders(<AppShell />, { initialEntries: ['/pipelets'] })

    expect(screen.getByRole('link', { name: 'Pipelets' })).toHaveClass('active')
    expect(screen.getByRole('heading', { name: 'Pipelets' })).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Pipelines' }))
    expect(screen.getByRole('heading', { name: 'Pipelines' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Pipelines' })).toHaveClass('active')

    await user.click(screen.getByRole('link', { name: 'Tenants' }))
    expect(screen.getByRole('heading', { name: 'Tenants' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tenants' })).toHaveClass('active')

    await user.click(screen.getByRole('link', { name: 'Billing' }))
    expect(screen.getByRole('heading', { name: 'Billing' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Billing' })).toHaveClass('active')
  })

  it('shows tenant picker with stub tenants', async () => {
    renderWithProviders(<AppShell />, { initialEntries: ['/connectors'] })
    const select = screen.getByRole('combobox', { name: 'Tenant' })
    expect(select).toHaveValue('T001')
    await waitFor(() => {
      expect(
        screen.getByRole('option', { name: /Acme Analytics/ }),
      ).toBeInTheDocument()
    })
  })
})
