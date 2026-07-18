import { useEffect, useState } from 'react'
import { pipelineGuide } from '../../api/client'
import type {
  AgentModelOption,
  PipelineGuideResponse,
  ProposedPipeline,
} from '../../api/types'
import { ModelPicker, modelKey, parseModelKey } from '../shared/ModelPicker'

type Turn = {
  role: 'user' | 'assistant'
  text: string
  thinking?: string | null
  proposed?: ProposedPipeline | null
  raw?: string | null
}

type Props = {
  models: AgentModelOption[]
  modelsNote?: string | null
  defaultKey: string
}

export function PipelineGuidePanel({ models, modelsNote, defaultKey }: Props) {
  const [selectedKey, setSelectedKey] = useState(defaultKey)
  const [apiKey, setApiKey] = useState('')
  const [input, setInput] = useState('')
  const [turns, setTurns] = useState<Turn[]>([])
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [latest, setLatest] = useState<ProposedPipeline | null>(null)
  const [showRaw, setShowRaw] = useState(true)

  useEffect(() => {
    if (models.length) setSelectedKey(defaultKey)
  }, [models, defaultKey])

  const { provider, id } = parseModelKey(selectedKey)
  const needsKey = provider === 'openai' || provider === 'anthropic'

  async function send() {
    const message = input.trim()
    if (!message || pending) return
    setError(null)
    setPending(true)
    setInput('')
    setTurns((t) => [...t, { role: 'user', text: message }])
    try {
      const res: PipelineGuideResponse = await pipelineGuide({
        message,
        provider,
        model: id,
        api_key: needsKey && apiKey.trim() ? apiKey.trim() : undefined,
      })
      setLatest(res.proposed_pipeline ?? null)
      setTurns((t) => [
        ...t,
        {
          role: 'assistant',
          text: res.reply,
          thinking: res.thinking,
          proposed: res.proposed_pipeline,
          raw: res.raw_model_output,
        },
      ])
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Request failed'
      setError(msg)
      setTurns((t) => [...t, { role: 'assistant', text: msg }])
    } finally {
      setPending(false)
    }
  }

  function exportJson() {
    if (!latest) return
    const blob = new Blob([JSON.stringify(latest, null, 2)], {
      type: 'application/json',
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${latest.name.replace(/\s+/g, '-').toLowerCase() || 'pipeline'}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <section className="panel" data-testid="pipeline-guide">
      <p className="muted">
        Design Dataflow pipelines: describe Source → Processor → Destination flows.
        Export JSON and import into the Dataflow builder.
      </p>
      {modelsNote ? <p className="muted note">{modelsNote}</p> : null}
      <ModelPicker
        models={models}
        selectedKey={selectedKey}
        onChange={setSelectedKey}
        disabled={pending}
      />
      {needsKey ? (
        <label className="api-key">
          Session API key
          <input
            type="password"
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder="Not stored on server"
          />
        </label>
      ) : null}
      <label className="toggle">
        <input type="checkbox" checked={showRaw} onChange={(e) => setShowRaw(e.target.checked)} />
        Show thinking & raw output
      </label>
      <div className="log">
        {turns.map((turn, i) => (
          <div key={i} className={`turn ${turn.role}`}>
            <strong>{turn.role === 'user' ? 'You' : 'Guide'}</strong>
            {showRaw && turn.thinking ? <pre className="thinking">{turn.thinking}</pre> : null}
            <p>{turn.text}</p>
            {turn.proposed?.steps?.length ? (
              <ol>
                {turn.proposed.steps.map((s) => (
                  <li key={`${s.step_order}-${s.pipelet_id}`}>
                    {s.step_order}. {s.pipelet_id}
                  </li>
                ))}
              </ol>
            ) : null}
            {showRaw && turn.raw ? (
              <details>
                <summary>Raw output</summary>
                <pre>{turn.raw}</pre>
              </details>
            ) : null}
          </div>
        ))}
        {pending ? <p className="muted">Thinking…</p> : null}
      </div>
      {error ? <p className="error">{error}</p> : null}
      <textarea
        rows={3}
        value={input}
        disabled={pending}
        placeholder="e.g. Manual trigger that writes filtered JSON to S3"
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
            e.preventDefault()
            void send()
          }
        }}
      />
      <div className="actions">
        <button type="button" disabled={pending || !input.trim()} onClick={() => void send()}>
          Ask guide
        </button>
        <button type="button" disabled={!latest?.steps?.length} onClick={exportJson}>
          Export JSON
        </button>
      </div>
    </section>
  )
}

export { modelKey }
