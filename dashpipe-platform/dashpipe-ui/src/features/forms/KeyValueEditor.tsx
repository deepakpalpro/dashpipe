import { useState } from 'react'

type Props = {
  entries: Record<string, unknown>
  onChange: (next: Record<string, unknown>) => void
  title?: string
  /** When true, secret-looking keys use password inputs. */
  maskSecrets?: boolean
}

function isSecretKey(key: string): boolean {
  const k = key.toLowerCase()
  const compact = k.replace(/[-_]/g, '')
  return (
    k.includes('secret') ||
    k.includes('password') ||
    k === 'api_key' ||
    k.endsWith('_key') ||
    compact.includes('accesskey') ||
    compact === 'apikey'
  )
}

export function KeyValueEditor({
  entries,
  onChange,
  title = 'Additional config',
  maskSecrets = true,
}: Props) {
  const [draftKey, setDraftKey] = useState('')
  const [draftValue, setDraftValue] = useState('')

  const rows = Object.entries(entries)

  function setValue(key: string, value: string) {
    onChange({ ...entries, [key]: value })
  }

  function removeKey(key: string) {
    const next = { ...entries }
    delete next[key]
    onChange(next)
  }

  function addEntry() {
    const key = draftKey.trim()
    if (!key) {
      return
    }
    onChange({ ...entries, [key]: draftValue })
    setDraftKey('')
    setDraftValue('')
  }

  return (
    <div className="config-editor" aria-label={title}>
      <h3>{title}</h3>
      {rows.length === 0 ? (
        <p className="muted">No extra keys yet</p>
      ) : (
        <ul className="config-rows">
          {rows.map(([key, value]) => (
            <li key={key} className="config-row">
              <span className="config-key">{key}</span>
              <input
                aria-label={`Config value ${key}`}
                type={
                  maskSecrets && isSecretKey(key) ? 'password' : 'text'
                }
                autoComplete="off"
                value={value == null ? '' : String(value)}
                onChange={(e) => setValue(key, e.target.value)}
              />
              <button
                type="button"
                className="secondary"
                aria-label={`Remove config ${key}`}
                onClick={() => removeKey(key)}
              >
                ×
              </button>
            </li>
          ))}
        </ul>
      )}
      <div className="config-add">
        <input
          aria-label={`${title} key`}
          placeholder="key"
          value={draftKey}
          onChange={(e) => setDraftKey(e.target.value)}
        />
        <input
          aria-label={`${title} value`}
          placeholder="value"
          value={draftValue}
          onChange={(e) => setDraftValue(e.target.value)}
        />
        <button type="button" className="secondary" onClick={addEntry}>
          Add
        </button>
      </div>
    </div>
  )
}
