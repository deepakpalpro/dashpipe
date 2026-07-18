import { useState } from 'react'
import { pythonDeveloper } from '../../api/client'
import type { AgentModelOption, GeneratedFile } from '../../api/types'
import { ModelPicker, parseModelKey } from '../shared/ModelPicker'

type Props = {
  models: AgentModelOption[]
  codeModels: AgentModelOption[]
  defaultKey: string
}

export function CodeGeneratorPanel({ models, codeModels, defaultKey }: Props) {
  const pickerModels = codeModels.length ? codeModels : models
  const [selectedKey, setSelectedKey] = useState(defaultKey)
  const [apiKey, setApiKey] = useState('')
  const [requirement, setRequirement] = useState('')
  const [context, setContext] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [plainText, setPlainText] = useState('')
  const [files, setFiles] = useState<GeneratedFile[]>([])
  const [justification, setJustification] = useState('')

  const { provider, id } = parseModelKey(selectedKey)
  const needsKey = provider === 'openai' || provider === 'anthropic'

  async function generate() {
    const req = requirement.trim()
    if (!req || pending) return
    setError(null)
    setPending(true)
    try {
      const res = await pythonDeveloper({
        requirement: req,
        context: context.trim() || undefined,
        provider,
        model: id,
        api_key: needsKey && apiKey.trim() ? apiKey.trim() : undefined,
      })
      setPlainText(res.plain_text)
      setFiles(res.files)
      setJustification(res.model_justification)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Generation failed')
    } finally {
      setPending(false)
    }
  }

  function copyAll() {
    void navigator.clipboard.writeText(plainText)
  }

  return (
    <section className="panel" data-testid="code-generator">
      <p className="muted">
        Generate Dataflow pipelet Python (<code>logic.py</code>, <code>main.py</code>),
        Dockerfile, and Kubernetes Job manifests.
      </p>
      <ModelPicker
        models={pickerModels}
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
          />
        </label>
      ) : null}
      <label>
        Requirement
        <textarea
          rows={4}
          value={requirement}
          disabled={pending}
          placeholder="Pipelet that counts records in JSON input and adds recordCount"
          onChange={(e) => setRequirement(e.target.value)}
        />
      </label>
      <label>
        Context (optional)
        <textarea
          rows={2}
          value={context}
          disabled={pending}
          placeholder="Uses io_transport queue mode, plet-manual-source pattern"
          onChange={(e) => setContext(e.target.value)}
        />
      </label>
      <div className="actions">
        <button type="button" disabled={pending || !requirement.trim()} onClick={() => void generate()}>
          {pending ? 'Generating…' : 'Generate'}
        </button>
        {plainText ? (
          <button type="button" onClick={copyAll}>
            Copy all
          </button>
        ) : null}
      </div>
      {error ? <p className="error">{error}</p> : null}
      {justification ? <p className="muted note">{justification}</p> : null}
      {files.length > 0 ? (
        <div className="files">
          {files.map((f) => (
            <details key={f.path} open>
              <summary>{f.path}</summary>
              <pre>{f.content}</pre>
              <button type="button" onClick={() => void navigator.clipboard.writeText(f.content)}>
                Copy file
              </button>
            </details>
          ))}
        </div>
      ) : plainText ? (
        <pre className="output">{plainText}</pre>
      ) : null}
    </section>
  )
}
