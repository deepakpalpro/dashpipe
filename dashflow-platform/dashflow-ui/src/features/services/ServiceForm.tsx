import { useMemo, useState, type FormEvent } from 'react'
import type { CreateTenantServiceRequest, ServiceType } from '../../api/types'
import { KeyValueEditor } from '../forms/KeyValueEditor'
import { validateServiceForm, type FieldErrors } from '../forms/validation'

type Props = {
  serviceTypes: ServiceType[]
  onSubmit: (body: CreateTenantServiceRequest) => Promise<void> | void
  onCancel?: () => void
}

export function ServiceForm({ serviceTypes, onSubmit, onCancel }: Props) {
  const [serviceTypeId, setServiceTypeId] = useState(serviceTypes[0]?.id ?? '')
  const [vendor, setVendor] = useState(
    serviceTypes[0]?.defaults?.[0]?.vendor ?? '',
  )
  const [name, setName] = useState('')
  const [clientId, setClientId] = useState('')
  const [clientSecret, setClientSecret] = useState('')
  const [extraConfig, setExtraConfig] = useState<Record<string, unknown>>({})
  const [deploymentConfig, setDeploymentConfig] = useState<
    Record<string, unknown>
  >({ cloud: 'aws', region: 'us-east-1' })
  const [errors, setErrors] = useState<FieldErrors>({})
  const [submitting, setSubmitting] = useState(false)

  const vendors = useMemo(() => {
    const selected = serviceTypes.find((t) => t.id === serviceTypeId)
    return selected?.defaults?.map((d) => d.vendor) ?? []
  }, [serviceTypes, serviceTypeId])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const next = validateServiceForm({ serviceTypeId, vendor, name })
    setErrors(next)
    if (Object.keys(next).length > 0) {
      return
    }
    setSubmitting(true)
    try {
      const tenantConfig: Record<string, unknown> = { ...extraConfig }
      if (clientId.trim()) {
        tenantConfig.client_id = clientId.trim()
      }
      if (clientSecret.trim()) {
        tenantConfig.client_secret = clientSecret.trim()
      }
      await onSubmit({
        serviceTypeId,
        vendor: vendor.trim(),
        name: name.trim(),
        tenantConfig,
        deployment_config: deploymentConfig,
        execution_config: tenantConfig,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="entity-form" onSubmit={handleSubmit} noValidate>
      <h2>Create service</h2>

      <label>
        Type
        <select
          aria-label="Service type"
          value={serviceTypeId}
          onChange={(e) => {
            const nextType = e.target.value
            setServiceTypeId(nextType)
            const nextVendors =
              serviceTypes.find((t) => t.id === nextType)?.defaults?.map(
                (d) => d.vendor,
              ) ?? []
            setVendor(nextVendors[0] ?? '')
          }}
        >
          <option value="">Select type</option>
          {serviceTypes.map((t) => (
            <option key={t.id} value={t.id}>
              {t.displayName}
            </option>
          ))}
        </select>
        {errors.serviceTypeId ? (
          <span role="alert" className="field-error">
            {errors.serviceTypeId}
          </span>
        ) : null}
      </label>

      <label>
        Vendor
        {vendors.length > 0 ? (
          <select
            aria-label="Vendor"
            value={vendor}
            onChange={(e) => setVendor(e.target.value)}
          >
            <option value="">Select vendor</option>
            {vendors.map((v) => (
              <option key={v} value={v}>
                {v}
              </option>
            ))}
          </select>
        ) : (
          <input
            aria-label="Vendor"
            value={vendor}
            onChange={(e) => setVendor(e.target.value)}
            placeholder="e.g. OAuth, Keycloak, AAD"
          />
        )}
        {errors.vendor ? (
          <span role="alert" className="field-error">
            {errors.vendor}
          </span>
        ) : null}
      </label>

      <label>
        Name
        <input
          aria-label="Service name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        {errors.name ? (
          <span role="alert" className="field-error">
            {errors.name}
          </span>
        ) : null}
      </label>

      <label>
        Client ID
        <input
          aria-label="Client ID"
          value={clientId}
          onChange={(e) => setClientId(e.target.value)}
        />
      </label>
      <label>
        Client secret
        <input
          aria-label="Client secret"
          type="password"
          autoComplete="off"
          value={clientSecret}
          onChange={(e) => setClientSecret(e.target.value)}
        />
      </label>

      <KeyValueEditor
        title="Deployment configuration"
        entries={deploymentConfig}
        onChange={setDeploymentConfig}
      />
      <KeyValueEditor
        title="Execution configuration"
        entries={extraConfig}
        onChange={setExtraConfig}
      />

      <div className="form-actions">
        <button type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create'}
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
