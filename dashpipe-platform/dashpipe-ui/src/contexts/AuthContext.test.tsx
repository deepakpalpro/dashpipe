import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useAuth } from '../contexts/AuthContext'
import { useTenant } from '../contexts/TenantContext'
import { renderWithProviders } from '../test/renderWithProviders'

function AuthConsumer() {
  const { isAuthenticated, displayName, signOutStub, signInStub } = useAuth()
  return (
    <div>
      <span data-testid="auth-state">
        {isAuthenticated ? displayName : 'anonymous'}
      </span>
      <button type="button" onClick={() => signOutStub()}>
        Sign out
      </button>
      <button type="button" onClick={() => signInStub('Ada')}>
        Sign in
      </button>
    </div>
  )
}

function TenantConsumer() {
  const { tenantId, setTenantId } = useTenant()
  return (
    <div>
      <span data-testid="tenant-id">{tenantId}</span>
      <button type="button" onClick={() => setTenantId('T002')}>
        Switch tenant
      </button>
    </div>
  )
}

describe('AuthContext', () => {
  it('provides a stub authenticated session by default', () => {
    renderWithProviders(<AuthConsumer />)
    expect(screen.getByTestId('auth-state')).toHaveTextContent(
      'Platform Operator',
    )
  })

  it('signOutStub clears the session', async () => {
    const user = userEvent.setup()
    renderWithProviders(<AuthConsumer />)
    await user.click(screen.getByRole('button', { name: 'Sign out' }))
    expect(screen.getByTestId('auth-state')).toHaveTextContent('anonymous')
  })

  it('signInStub restores a display name', async () => {
    const user = userEvent.setup()
    renderWithProviders(<AuthConsumer />)
    await user.click(screen.getByRole('button', { name: 'Sign out' }))
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(screen.getByTestId('auth-state')).toHaveTextContent('Ada')
  })
})

describe('TenantContext', () => {
  it('defaults to T001', () => {
    renderWithProviders(<TenantConsumer />)
    expect(screen.getByTestId('tenant-id')).toHaveTextContent('T001')
  })

  it('switch tenant updates consumers', async () => {
    const user = userEvent.setup()
    renderWithProviders(<TenantConsumer />)
    await user.click(screen.getByRole('button', { name: 'Switch tenant' }))
    expect(screen.getByTestId('tenant-id')).toHaveTextContent('T002')
  })
})
