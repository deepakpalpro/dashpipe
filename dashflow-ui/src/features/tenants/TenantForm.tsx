import { useState, type FormEvent } from 'react'
import type { CreateTenantRequest, TenantStatus } from '../../api/types'

type Props = {
  onSubmit: (body: CreateTenantRequest) => Promise<void> | void
  onCancel?: () => void
}

const STATUSES: TenantStatus[] = ['trial', 'active', 'suspended']

function slugify(name: string): string {
  return name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 64)
}

export function TenantForm({ onSubmit, onCancel }: Props) {
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [slugTouched, setSlugTouched] = useState(false)
  const [status, setStatus] = useState<TenantStatus>('trial')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const next: Record<string, string> = {}
    if (!name.trim()) {
      next.name = 'Name is required'
    }
    if (!slug.trim()) {
      next.slug = 'Slug is required'
    }
    setErrors(next)
    if (Object.keys(next).length > 0) {
      return
    }
    setSubmitting(true)
    try {
      await onSubmit({
        name: name.trim(),
        slug: slug.trim().toLowerCase(),
        status,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="entity-form" onSubmit={handleSubmit} noValidate>
      <h2>Register tenant</h2>
      <p className="muted">
        Creates a platform tenant via <code>POST /api/v1/tenants</code>. New
        tenants start with prepaid credit for pipeline runs.
      </p>

      <label>
        Name
        <input
          aria-label="Tenant name"
          value={name}
          onChange={(e) => {
            const value = e.target.value
            setName(value)
            if (!slugTouched) {
              setSlug(slugify(value))
            }
          }}
        />
        {errors.name ? (
          <span role="alert" className="field-error">
            {errors.name}
          </span>
        ) : null}
      </label>

      <label>
        Slug
        <input
          aria-label="Tenant slug"
          value={slug}
          onChange={(e) => {
            setSlugTouched(true)
            setSlug(e.target.value)
          }}
          placeholder="my-tenant"
        />
        {errors.slug ? (
          <span role="alert" className="field-error">
            {errors.slug}
          </span>
        ) : null}
      </label>

      <label>
        Status
        <select
          aria-label="Tenant status"
          value={status}
          onChange={(e) => setStatus(e.target.value as TenantStatus)}
        >
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </label>

      <div className="form-actions">
        <button type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create tenant'}
        </button>
        {onCancel ? (
          <button type="button" className="secondary" onClick={onCancel}>
            Cancel
          </button>
        ) : null}
      </div>
    </form>
  )
}
