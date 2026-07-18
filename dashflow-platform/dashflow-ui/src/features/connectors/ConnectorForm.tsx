import { useState, type FormEvent } from 'react'
import type { ConnectorType, CreateConnectorRequest } from '../../api/types'
import { KeyValueEditor } from '../forms/KeyValueEditor'
import { validateConnectorForm, type FieldErrors } from '../forms/validation'

type Props = {
  connectorTypes: ConnectorType[]
  onSubmit: (body: CreateConnectorRequest) => Promise<void> | void
  onCancel?: () => void
}

export function ConnectorForm({ connectorTypes, onSubmit, onCancel }: Props) {
  const [connectorTypeId, setConnectorTypeId] = useState(
    connectorTypes[0]?.id ?? '',
  )
  const [name, setName] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [extraConfig, setExtraConfig] = useState<Record<string, unknown>>({})
  const [deploymentConfig, setDeploymentConfig] = useState<Record<string, unknown>>({ cloud: 'aws', region: 'us-east-1' })
  const [errors, setErrors] = useState<FieldErrors>({})
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const next = validateConnectorForm({ connectorTypeId, name, baseUrl })
    setErrors(next)
    if (Object.keys(next).length > 0) {
      return
    }
    setSubmitting(true)
    try {
      const config: Record<string, unknown> = {
        ...extraConfig,
        baseUrl: baseUrl.trim(),
      }
      if (apiKey.trim()) {
        config.api_key = apiKey.trim()
      }
      await onSubmit({
        connectorTypeId,
        name: name.trim(),
        config,
        deployment_config: deploymentConfig,
        execution_config: config,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="entity-form" onSubmit={handleSubmit} noValidate>
      <h2>Create connector</h2>

      <label>
        Type
        <select
          aria-label="Connector type"
          value={connectorTypeId}
          onChange={(e) => setConnectorTypeId(e.target.value)}
        >
          <option value="">Select type</option>
          {connectorTypes.map((t) => (
            <option key={t.id} value={t.id}>
              {t.displayName}
            </option>
          ))}
        </select>
        {errors.connectorTypeId ? (
          <span role="alert" className="field-error">
            {errors.connectorTypeId}
          </span>
        ) : null}
      </label>

      <label>
        Name
        <input
          aria-label="Connector name"
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
        Base URL
        <input
          aria-label="Base URL"
          value={baseUrl}
          onChange={(e) => setBaseUrl(e.target.value)}
        />
        {errors.baseUrl ? (
          <span role="alert" className="field-error">
            {errors.baseUrl}
          </span>
        ) : null}
      </label>

      <label>
        API key (optional)
        <input
          aria-label="API key"
          type="password"
          autoComplete="off"
          value={apiKey}
          onChange={(e) => setApiKey(e.target.value)}
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
          {submitting ? 'Saving…' : 'Create'}
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
