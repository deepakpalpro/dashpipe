import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { listTenants } from '../api/resources'
import type { Tenant } from '../api/types'

export const TENANT_STORAGE_KEY = 'pipeline.tenantId'
export const DEFAULT_TENANT_ID = 'T001'

/** Fallback when the tenants API is unavailable (offline / first paint). */
export const FALLBACK_TENANTS: Tenant[] = [
  {
    id: 'T001',
    name: 'Acme Analytics',
    slug: 'acme-analytics',
    status: 'active',
    creditBalance: 100,
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-07-01T00:00:00Z',
  },
  {
    id: 'T002',
    name: 'Beta Logistics',
    slug: 'beta-logistics',
    status: 'active',
    creditBalance: 100,
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-07-01T00:00:00Z',
  },
]

/** @deprecated Prefer FALLBACK_TENANTS; kept for existing imports/tests. */
export const STUB_TENANTS = FALLBACK_TENANTS.map((t) => ({
  id: t.id,
  name: t.name,
}))

export type TenantListItem = {
  id: string
  name: string
  slug?: string
  status?: string
}

type TenantContextValue = {
  tenantId: string
  tenantName: string
  setTenantId: (id: string) => void
  tenants: TenantListItem[]
  tenantsLoading: boolean
  tenantsError: boolean
  refreshTenants: () => Promise<void>
}

const TenantContext = createContext<TenantContextValue | null>(null)

function readStoredTenantId(): string {
  try {
    const stored = sessionStorage.getItem(TENANT_STORAGE_KEY)
    if (stored) {
      return stored
    }
  } catch {
    /* ignore */
  }
  return DEFAULT_TENANT_ID
}

export function TenantProvider({
  children,
  initialTenantId,
}: {
  children: ReactNode
  initialTenantId?: string
}) {
  const queryClient = useQueryClient()
  const [tenantId, setTenantIdState] = useState(
    () => initialTenantId ?? readStoredTenantId(),
  )

  const tenantsQuery = useQuery({
    queryKey: ['tenants'],
    queryFn: () => listTenants(tenantId || DEFAULT_TENANT_ID),
    staleTime: 15_000,
  })

  const tenants: TenantListItem[] = useMemo(() => {
    if (tenantsQuery.data && tenantsQuery.data.length > 0) {
      return tenantsQuery.data.map((t) => ({
        id: t.id,
        name: t.name,
        slug: t.slug,
        status: t.status,
      }))
    }
    return FALLBACK_TENANTS.map((t) => ({
      id: t.id,
      name: t.name,
      slug: t.slug,
      status: t.status,
    }))
  }, [tenantsQuery.data])

  const setTenantId = useCallback((id: string) => {
    setTenantIdState(id)
    try {
      sessionStorage.setItem(TENANT_STORAGE_KEY, id)
    } catch {
      /* ignore */
    }
  }, [])

  useEffect(() => {
    if (tenants.length === 0) {
      return
    }
    if (!tenants.some((t) => t.id === tenantId)) {
      setTenantId(tenants[0].id)
    }
  }, [tenants, tenantId, setTenantId])

  const tenantName =
    tenants.find((t) => t.id === tenantId)?.name ?? tenantId

  const refreshTenants = useCallback(async () => {
    await queryClient.invalidateQueries({ queryKey: ['tenants'] })
  }, [queryClient])

  const value = useMemo(
    () => ({
      tenantId,
      tenantName,
      setTenantId,
      tenants,
      tenantsLoading: tenantsQuery.isLoading,
      tenantsError: tenantsQuery.isError,
      refreshTenants,
    }),
    [
      tenantId,
      tenantName,
      setTenantId,
      tenants,
      tenantsQuery.isLoading,
      tenantsQuery.isError,
      refreshTenants,
    ],
  )

  return (
    <TenantContext.Provider value={value}>{children}</TenantContext.Provider>
  )
}

export function useTenant(): TenantContextValue {
  const ctx = useContext(TenantContext)
  if (!ctx) {
    throw new Error('useTenant must be used within TenantProvider')
  }
  return ctx
}
