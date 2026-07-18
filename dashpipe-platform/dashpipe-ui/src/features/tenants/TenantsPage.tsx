import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { createTenant, listTenants } from '../../api/resources'
import type { CreateTenantRequest, Tenant } from '../../api/types'
import { useTenant } from '../../contexts/TenantContext'
import { TenantForm } from './TenantForm'

function money(value: number | string | null | undefined): string {
  if (value == null || value === '') {
    return '—'
  }
  const n = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(n)) {
    return String(value)
  }
  return `$${n.toFixed(2)}`
}

export function TenantsPage() {
  const { tenantId, setTenantId, refreshTenants } = useTenant()
  const queryClient = useQueryClient()
  const [creating, setCreating] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const tenantsQuery = useQuery({
    queryKey: ['tenants'],
    queryFn: () => listTenants(tenantId),
  })

  const createMutation = useMutation({
    mutationFn: (body: CreateTenantRequest) => createTenant(tenantId, body),
    onSuccess: async (created) => {
      await queryClient.invalidateQueries({ queryKey: ['tenants'] })
      await refreshTenants()
      setCreating(false)
      setTenantId(created.id)
      setMessage(`Created ${created.name} and switched context to ${created.id}`)
    },
    onError: (err) => {
      setMessage(err instanceof Error ? err.message : 'Create failed')
    },
  })

  const tenants: Tenant[] = tenantsQuery.data ?? []

  return (
    <section className="table-page" aria-label="Tenants">
      <div className="panel-header">
        <div>
          <h1>Tenants</h1>
          <p className="muted billing-subtitle">
            Register a tenant, then switch context from the header picker.
          </p>
        </div>
        <button type="button" onClick={() => setCreating(true)}>
          New tenant
        </button>
      </div>

      {creating ? (
        <TenantForm
          onCancel={() => setCreating(false)}
          onSubmit={async (body) => {
            setMessage(null)
            await createMutation.mutateAsync(body)
          }}
        />
      ) : null}

      {tenantsQuery.isLoading ? <p className="muted">Loading…</p> : null}
      {tenantsQuery.isError ? (
        <p role="alert">Failed to load tenants</p>
      ) : null}

      <table className="entity-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Slug</th>
            <th>Status</th>
            <th>Credit</th>
            <th>Id</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {tenants.map((t) => (
            <tr
              key={t.id}
              className={t.id === tenantId ? 'row-active' : undefined}
              data-testid={`tenant-row-${t.id}`}
            >
              <td>{t.name}</td>
              <td>
                <code>{t.slug}</code>
              </td>
              <td>{t.status}</td>
              <td>{money(t.creditBalance)}</td>
              <td>
                <code>{t.id}</code>
              </td>
              <td>
                {t.id === tenantId ? (
                  <span className="muted">Current</span>
                ) : (
                  <button
                    type="button"
                    className="secondary"
                    aria-label={`Switch to ${t.name}`}
                    onClick={() => {
                      setTenantId(t.id)
                      setMessage(`Switched to ${t.name}`)
                    }}
                  >
                    Switch
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {message ? (
        <p role="status" data-testid="tenant-page-status">
          {message}
        </p>
      ) : null}
    </section>
  )
}
