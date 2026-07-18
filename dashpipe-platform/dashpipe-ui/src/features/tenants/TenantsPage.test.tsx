import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TenantForm } from './TenantForm'
import { TenantsPage } from './TenantsPage'
import { renderWithProviders } from '../../test/renderWithProviders'

describe('TenantForm', () => {
  it('blocks submit without name and slug', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    renderWithProviders(<TenantForm onSubmit={onSubmit} />)

    await user.click(screen.getByRole('button', { name: 'Create tenant' }))
    expect(screen.getByText('Name is required')).toBeInTheDocument()
    expect(screen.getByText('Slug is required')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('auto-fills slug from name and submits', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderWithProviders(<TenantForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('Tenant name'), 'Gamma Corp')
    expect(screen.getByLabelText('Tenant slug')).toHaveValue('gamma-corp')
    await user.selectOptions(screen.getByLabelText('Tenant status'), 'active')
    await user.click(screen.getByRole('button', { name: 'Create tenant' }))

    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Gamma Corp',
      slug: 'gamma-corp',
      status: 'active',
    })
  })
})

describe('TenantsPage', () => {
  it('lists seeded tenants and registers a new one', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TenantsPage />, { initialEntries: ['/tenants'] })

    await waitFor(() => {
      expect(screen.getByTestId('tenant-row-T001')).toBeInTheDocument()
    })
    expect(screen.getByText('Acme Analytics')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'New tenant' }))
    await user.type(screen.getByLabelText('Tenant name'), 'Gamma Corp')
    await user.click(screen.getByRole('button', { name: 'Create tenant' }))

    await waitFor(() => {
      expect(screen.getByTestId('tenant-page-status').textContent).toMatch(
        /Created Gamma Corp/,
      )
    })
    expect(screen.getByText('Gamma Corp')).toBeInTheDocument()
  })
})
