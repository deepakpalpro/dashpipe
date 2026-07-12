import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ServiceForm } from './ServiceForm'
import { renderWithProviders } from '../../test/renderWithProviders'

const types = [
  {
    id: 'st-auth',
    type: 'AUTH',
    displayName: 'Auth',
    defaults: [
      { id: 'sd-auth-stub', vendor: 'StubAuth' },
      { id: 'sd-auth-oauth', vendor: 'OAuth' },
    ],
  },
]

describe('ServiceForm', () => {
  it('blocks submit without vendor', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    renderWithProviders(
      <ServiceForm serviceTypes={types} onSubmit={onSubmit} />,
    )

    await user.selectOptions(screen.getByLabelText('Vendor'), '')
    await user.type(screen.getByLabelText('Service name'), 'Auth A')
    await user.click(screen.getByRole('button', { name: 'Create' }))

    expect(screen.getByText('Vendor is required')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits when vendor and name are set', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderWithProviders(
      <ServiceForm serviceTypes={types} onSubmit={onSubmit} />,
    )

    await user.selectOptions(screen.getByLabelText('Vendor'), 'OAuth')
    await user.type(screen.getByLabelText('Service name'), 'Auth A')
    await user.click(screen.getByRole('button', { name: 'Create' }))

    expect(onSubmit).toHaveBeenCalledWith({
      serviceTypeId: 'st-auth',
      vendor: 'OAuth',
      name: 'Auth A',
      tenantConfig: {},
      deployment_config: { cloud: 'aws', region: 'us-east-1' },
      execution_config: {},
    })
  })
})
