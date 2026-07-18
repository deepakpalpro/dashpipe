type Props = {
  canRun: boolean
  saving?: boolean
  activating?: boolean
  deactivating?: boolean
  running?: boolean
  canForceStop?: boolean
  stopping?: boolean
  status?: string | null
  /** Show optional JSON body editor when the graph starts with Manual Source. */
  showTriggerPayload?: boolean
  triggerPayloadText?: string
  onTriggerPayloadChange?: (value: string) => void
  onDryRun: () => void
  onSave: () => void
  onActivate: () => void
  onDeactivate: () => void
  onRun: () => void
  onForceStop?: () => void
}

function normalizeStatus(status?: string | null): string {
  return (status ?? 'DRAFT').toUpperCase()
}

export function RunControls({
  canRun,
  saving,
  activating,
  deactivating,
  running,
  canForceStop,
  stopping,
  status,
  showTriggerPayload,
  triggerPayloadText,
  onTriggerPayloadChange,
  onDryRun,
  onSave,
  onActivate,
  onDeactivate,
  onRun,
  onForceStop,
}: Props) {
  const normalized = normalizeStatus(status)
  const isActive = normalized === 'ACTIVE'
  const busy = Boolean(
    saving || activating || deactivating || running || stopping,
  )

  return (
    <div className="run-controls-stack" aria-label="Run controls">
      <div className="run-controls">
        <span
          className={
            isActive ? 'pipeline-status-badge active' : 'pipeline-status-badge'
          }
          data-testid="pipeline-status"
        >
          {normalized}
        </span>
        <button
          type="button"
          className="secondary"
          onClick={onDryRun}
          disabled={!canRun || busy}
        >
          Dry Run
        </button>
        <button
          type="button"
          className="secondary"
          onClick={onSave}
          disabled={busy || !canRun}
        >
          {saving ? 'Saving…' : 'Save'}
        </button>
        {isActive ? (
          <button
            type="button"
            className="secondary"
            onClick={onDeactivate}
            disabled={!canRun || busy}
            title="Set pipeline to DRAFT so schedules and deploy gates stop"
            data-testid="pipeline-deactivate"
          >
            {deactivating ? 'Deactivating…' : 'Deactivate'}
          </button>
        ) : (
          <button
            type="button"
            onClick={onActivate}
            disabled={!canRun || busy}
            title="Save and set status ACTIVE so schedules and Run can proceed"
            data-testid="pipeline-activate"
          >
            {activating ? 'Activating…' : 'Activate'}
          </button>
        )}
        <button type="button" onClick={onRun} disabled={!canRun || busy}>
          {running ? 'Running…' : 'Run'}
        </button>
        {onForceStop ? (
          <button
            type="button"
            className="danger"
            onClick={onForceStop}
            disabled={!canForceStop || Boolean(stopping)}
            title="Cancel the live execution, delete Jobs, purge queues, then Run fresh"
            data-testid="force-stop"
          >
            {stopping ? 'Stopping…' : 'Force Stop'}
          </button>
        ) : null}
      </div>
      {showTriggerPayload ? (
        <label className="trigger-payload-field">
          <span>Trigger payload (JSON, optional)</span>
          <textarea
            aria-label="Trigger payload JSON"
            data-testid="trigger-payload"
            rows={4}
            placeholder='{"sku":"food-01","qty":3}'
            value={triggerPayloadText ?? ''}
            onChange={(e) => onTriggerPayloadChange?.(e.target.value)}
            disabled={busy}
          />
          <span className="muted props-hint">
            Sent to Manual Source when you click Run. Leave empty for the
            default kickoff envelope.
          </span>
        </label>
      ) : null}
    </div>
  )
}
