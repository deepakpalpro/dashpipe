import { useEffect } from 'react'
import type { PipeletCatalogEntry } from './catalogFilter'

type Props = {
  pipelet: PipeletCatalogEntry | null
  onClose: () => void
}

function ConfigBlock({
  title,
  value,
}: {
  title: string
  value?: Record<string, unknown>
}) {
  if (!value || Object.keys(value).length === 0) {
    return null
  }
  return (
    <div className="detail-section">
      <h3>{title}</h3>
      <pre className="schema-preview">{JSON.stringify(value, null, 2)}</pre>
    </div>
  )
}

export function PipeletDetailModal({ pipelet, onClose }: Props) {
  useEffect(() => {
    if (!pipelet) {
      return
    }
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [pipelet, onClose])

  if (!pipelet) {
    return null
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onClick={onClose}
      data-testid="pipelet-detail-backdrop"
    >
      <div
        className="modal modal-wide"
        role="dialog"
        aria-modal="true"
        aria-label="Pipelet detail"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="modal-header">
          <div className="pipelet-detail-title">
            <h2>{pipelet.name}</h2>
            <div className="pipelet-card-meta">
              <span className="badge category">{pipelet.category}</span>
              <span
                className={
                  pipelet.active === true
                    ? 'badge status-active'
                    : 'badge status-inactive'
                }
              >
                {pipelet.active === true ? 'Active' : 'Inactive'}
              </span>
              <span className="badge runtime">{pipelet.runtime}</span>
              <span className="version">v{pipelet.version}</span>
              <code className="pipelet-id">{pipelet.id}</code>
            </div>
          </div>
          <button type="button" className="secondary" onClick={onClose}>
            Close
          </button>
        </header>

        <p className="pipelet-detail-desc">{pipelet.description}</p>
        {pipelet.imageRef ? (
          <p className="muted">
            Image: <code>{pipelet.imageRef}</code>
          </p>
        ) : null}
        {pipelet.requiredDeploymentKeys &&
        pipelet.requiredDeploymentKeys.length > 0 ? (
          <p className="muted">
            Required deployment Keys:{' '}
            {pipelet.requiredDeploymentKeys.join(', ')}
          </p>
        ) : null}
        {pipelet.requiredExecutionKeys &&
        pipelet.requiredExecutionKeys.length > 0 ? (
          <p className="muted">
            Required execution Keys: {pipelet.requiredExecutionKeys.join(', ')}
          </p>
        ) : null}

        <ConfigBlock
          title="Config schema"
          value={pipelet.configSchemaPreview}
        />
        <ConfigBlock
          title="Deployment configuration"
          value={pipelet.deploymentConfiguration}
        />
        <ConfigBlock
          title="Execution configuration"
          value={pipelet.executionConfiguration}
        />
      </div>
    </div>
  )
}
