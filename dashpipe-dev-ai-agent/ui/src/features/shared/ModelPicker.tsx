import type { AgentModelOption } from '../../api/types'

type Props = {
  models: AgentModelOption[]
  selectedKey: string
  onChange: (key: string) => void
  disabled?: boolean
}

export function modelKey(m: AgentModelOption): string {
  return `${m.provider}::${m.id}`
}

export function ModelPicker({ models, selectedKey, onChange, disabled }: Props) {
  const selected = models.find((m) => modelKey(m) === selectedKey) ?? models[0]
  return (
    <div className="model-row">
      <label>
        Model
        <select
          value={selected ? modelKey(selected) : selectedKey}
          disabled={disabled || models.length === 0}
          onChange={(e) => onChange(e.target.value)}
          data-testid="model-picker"
        >
          {models.map((m) => (
            <option key={modelKey(m)} value={modelKey(m)}>
              {m.label}
              {!m.configured ? ' (key needed)' : ''}
            </option>
          ))}
        </select>
      </label>
      {selected?.hint ? <span className="muted hint">{selected.hint}</span> : null}
    </div>
  )
}

export function parseModelKey(key: string): { provider: string; id: string } {
  const [provider, ...rest] = key.split('::')
  return { provider: provider ?? 'ollama', id: rest.join('::') }
}
