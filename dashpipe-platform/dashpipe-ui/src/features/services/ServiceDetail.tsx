import { useEffect, useState } from 'react'
import { displayConfigValue, isSecretKey, REDACTED } from '../../api/secrets'
import type { TenantService, UpdateTenantServiceRequest } from '../../api/types'
import { KeyValueEditor } from '../forms/KeyValueEditor'

type Props = {
  service: TenantService
  onSave?: (id: string, body: UpdateTenantServiceRequest) => Promise<void> | void
  saving?: boolean
}

function splitServiceConfig(config: Record<string, unknown>) {
  const { client_id: clientId, client_secret: clientSecret, ...extra } = config
  return {
    clientId: clientId == null ? '' : String(clientId),
    clientSecret:
      clientSecret == null ||
      clientSecret === REDACTED ||
      String(clientSecret) === '***'
        ? ''
        : String(clientSecret),
    hadSecret: clientSecret != null && String(clientSecret).length > 0,
    extra: Object.fromEntries(
      Object.entries(extra).map(([k, v]) => [
        k,
        isSecretKey(k) && (v === REDACTED || v === '***') ? '' : v,
      ]),
    ),
  }
}

function ConfigMap({
  title,
  entries,
}: {
  title: string
  entries: Record<string, unknown>
}) {
  const rows = Object.entries(entries)
  return (
    <div>
      <h3>{title}</h3>
      {rows.length === 0 ? (
        <p className="muted">No keys</p>
      ) : (
        <dl className="config-list">
          {rows.map(([key, value]) => (
            <div key={key}>
              <dt>{key}</dt>
              <dd data-testid={`config-${key}`}>{displayConfigValue(key, value)}</dd>
            </div>
          ))}
        </dl>
      )}
    </div>
  )
}

export function ServiceDetail({ service, onSave, saving }: Props) {
  const [name, setName] = useState(service.name)
  const [clientId, setClientId] = useState('')
  const [clientSecret, setClientSecret] = useState('')
  const [hadSecret, setHadSecret] = useState(false)
  const [extra, setExtra] = useState<Record<string, unknown>>({})
  const [deploymentConfig, setDeploymentConfig] = useState<Record<string, unknown>>({})
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    setName(service.name)
    const execution = service.execution_config ?? service.config ?? {}
    const split = splitServiceConfig(execution)
    setClientId(split.clientId)
    setClientSecret(split.clientSecret)
    setHadSecret(split.hadSecret)
    setExtra(split.extra)
    setDeploymentConfig({ ...(service.deployment_config ?? {}) })
    setMessage(null)
  }, [service.id, service.name, service.config, service.deployment_config, service.execution_config])

  async function handleSave() {
    if (!onSave) {
      return
    }
    const tenantConfig: Record<string, unknown> = { ...extra }
    if (clientId.trim()) {
      tenantConfig.client_id = clientId.trim()
    }
    if (clientSecret.trim()) {
      tenantConfig.client_secret = clientSecret.trim()
    } else if (hadSecret) {
      tenantConfig.client_secret = REDACTED
    }
    setMessage(null)
    try {
      await onSave(service.id, {
        name: name.trim() || service.name,
        tenantConfig,
        deployment_config: deploymentConfig,
        execution_config: tenantConfig,
      })
      setMessage('Saved')
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Save failed')
    }
  }

  if (!onSave) {
    return (
      <article className="entity-detail" aria-label="Service detail">
        <h2>{service.name}</h2>
        <dl>
          <div>
            <dt>ID</dt>
            <dd>{service.id}</dd>
          </div>
          <div>
            <dt>Vendor</dt>
            <dd>{service.vendor}</dd>
          </div>
          <div>
            <dt>Type</dt>
            <dd>{service.serviceTypeId}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{service.status}</dd>
          </div>
        </dl>
        <ConfigMap
          title="Deployment configuration"
          entries={service.deployment_config ?? {}}
        />
        <ConfigMap
          title="Execution configuration"
          entries={service.execution_config ?? service.config ?? {}}
        />
      </article>
    )
  }

  return (
    <article className="entity-detail entity-form" aria-label="Service detail">
      <h2>Edit service</h2>
      <p className="muted">
        {service.id} · {service.vendor}
      </p>

      <label>
        Name
        <input
          aria-label="Service name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
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
          placeholder={hadSecret ? '•••••• (leave blank to keep)' : ''}
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
        entries={extra}
        onChange={setExtra}
      />

      <div className="form-actions">
        <button type="button" onClick={() => void handleSave()} disabled={saving}>
          {saving ? 'Saving…' : 'Save changes'}
        </button>
      </div>
      {message ? (
        <p role="status" data-testid="service-save-status">
          {message}
        </p>
      ) : null}
    </article>
  )
}
