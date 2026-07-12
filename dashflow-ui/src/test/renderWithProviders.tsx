import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../contexts/AuthContext'
import { TenantProvider } from '../contexts/TenantContext'

type Options = {
  initialEntries?: string[]
  tenantId?: string
}

function Providers({
  children,
  tenantId,
}: {
  children: ReactNode
  tenantId?: string
}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <TenantProvider initialTenantId={tenantId}>{children}</TenantProvider>
      </AuthProvider>
    </QueryClientProvider>
  )
}

export function renderWithProviders(ui: ReactElement, options: Options = {}) {
  const { initialEntries = ['/'], tenantId } = options
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <Providers tenantId={tenantId}>{ui}</Providers>
    </MemoryRouter>,
  )
}
