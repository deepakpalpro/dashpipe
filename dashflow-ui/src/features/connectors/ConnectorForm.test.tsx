import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConnectorForm } from './ConnectorForm'
import { renderWithProviders } from '../../test/renderWithProviders'

const types = [
  {
    id: 'ct-rest',
    type: 'REST',
    displayName: 'REST',
    configSchema: null,
    spiClass: 'x',
    spiVersion: '1',
  },
]

describe('ConnectorForm', () => {
  it('shows validation errors when required fields are empty', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    renderWithProviders(
      <ConnectorForm connectorTypes={types} onSubmit={onSubmit} />,
    )

    await user.selectOptions(screen.getByLabelText('Connector type'), '')
    await user.click(screen.getByRole('button', { name: 'Create' }))

    expect(screen.getByText('Connector type is required')).toBeInTheDocument()
    expect(screen.getByText('Name is required')).toBeInTheDocument()
    expect(screen.getByText('Base URL is required')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits a valid payload', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderWithProviders(
      <ConnectorForm connectorTypes={types} onSubmit={onSubmit} />,
    )

    await user.type(screen.getByLabelText('Connector name'), 'Billing REST')
    await user.type(screen.getByLabelText('Base URL'), 'https://api.example.com')
    await user.type(screen.getByLabelText('API key'), 'temp-key')
    await user.click(screen.getByRole('button', { name: 'Create' }))

    expect(onSubmit).toHaveBeenCalledWith({
      connectorTypeId: 'ct-rest',
      name: 'Billing REST',
      config: { baseUrl: 'https://api.example.com', api_key: 'temp-key' },
      deployment_config: { cloud: 'aws', region: 'us-east-1' },
      execution_config: {
        baseUrl: 'https://api.example.com',
        api_key: 'temp-key',
      },
    })
  })
})
