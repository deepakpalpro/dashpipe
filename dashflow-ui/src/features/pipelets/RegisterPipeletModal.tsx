import { useState, type FormEvent } from 'react'

export type RegisterMode = 'imagePath' | 'imageUrl' | 'runtimeBinary'

export type RegisterPipeletInput = {
  mode: RegisterMode
  name: string
  category: string
  imageRef: string
}

type Props = {
  open: boolean
  onClose: () => void
  onSubmit: (input: RegisterPipeletInput) => Promise<void> | void
}

const TABS: { id: RegisterMode; label: string }[] = [
  { id: 'imagePath', label: 'Image Path' },
  { id: 'imageUrl', label: 'Image URL' },
  { id: 'runtimeBinary', label: 'Runtime Binary' },
]

export function RegisterPipeletModal({ open, onClose, onSubmit }: Props) {
  const [mode, setMode] = useState<RegisterMode>('imagePath')
  const [name, setName] = useState('')
  const [category, setCategory] = useState('Processor')
  const [imageRef, setImageRef] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!open) {
    return null
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!name.trim() || !imageRef.trim()) {
      setError('Name and image/binary reference are required')
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      await onSubmit({
        mode,
        name: name.trim(),
        category,
        imageRef: imageRef.trim(),
      })
      setName('')
      setImageRef('')
      onClose()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label="Register Pipelet"
      >
        <header className="modal-header">
          <h2>Register Pipelet</h2>
          <button type="button" className="secondary" onClick={onClose}>
            Close
          </button>
        </header>

        <div className="tab-row" role="tablist" aria-label="Register mode">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={mode === tab.id}
              className={mode === tab.id ? 'tab active' : 'tab'}
              onClick={() => setMode(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <form className="entity-form" onSubmit={handleSubmit} noValidate>
          <label>
            Name
            <input
              aria-label="Pipelet name"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </label>
          <label>
            Category
            <select
              aria-label="Pipelet category"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              <option value="Source">Source</option>
              <option value="Processor">Processor</option>
              <option value="Destination">Destination</option>
            </select>
          </label>
          <label>
            {mode === 'imageUrl'
              ? 'Image URL'
              : mode === 'runtimeBinary'
                ? 'Binary path'
                : 'Image path'}
            <input
              aria-label="Image reference"
              value={imageRef}
              onChange={(e) => setImageRef(e.target.value)}
              placeholder={
                mode === 'imageUrl'
                  ? 'https://…'
                  : mode === 'runtimeBinary'
                    ? '/opt/pipelets/…'
                    : 'registry.example/pipelets/…'
              }
            />
          </label>
          {error ? (
            <span role="alert" className="field-error">
              {error}
            </span>
          ) : null}
          <div className="form-actions">
            <button type="submit" disabled={submitting}>
              {submitting ? 'Registering…' : 'Register'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
