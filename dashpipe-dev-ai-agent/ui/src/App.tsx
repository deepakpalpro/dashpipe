import { useEffect, useState } from 'react'
import { fetchModels } from './api/client'
import type { AgentModelOption } from './api/types'
import { CodeGeneratorPanel } from './features/code-generator/CodeGeneratorPanel'
import { PipelineGuidePanel, modelKey } from './features/pipeline-guide/PipelineGuidePanel'
import './App.css'

type Tab = 'pipeline' | 'pipelet'

export default function App() {
  const [tab, setTab] = useState<Tab>('pipeline')
  const [models, setModels] = useState<AgentModelOption[]>([])
  const [note, setNote] = useState<string | null>(null)
  const [defaultKey, setDefaultKey] = useState('ollama::llama3.2:1b')

  useEffect(() => {
    void fetchModels()
      .then((res) => {
        setModels(res.models ?? [])
        setNote(res.note ?? null)
        const key = modelKey({
          provider: res.default_provider,
          id: res.default_model,
          label: '',
          configured: true,
        })
        setDefaultKey(key)
      })
      .catch(() => {
        setModels([
          {
            provider: 'ollama',
            id: 'llama3.2:1b',
            label: 'Ollama / llama3.2:1b',
            configured: true,
          },
        ])
      })
  }, [])

  const codeModels = models.filter(
    (m) =>
      m.provider === 'ollama' &&
      (m.id.includes('coder') || m.id.includes('1.5b') || m.id.includes('1b')),
  )

  return (
    <div className="app">
      <header>
        <h1>AI Dataflow Developer</h1>
        <p className="tagline">Pipelines · Pipelets · Python · Kubernetes</p>
      </header>
      <nav className="tabs">
        <button
          type="button"
          className={tab === 'pipeline' ? 'active' : ''}
          onClick={() => setTab('pipeline')}
        >
          Pipeline Guide
        </button>
        <button
          type="button"
          className={tab === 'pipelet' ? 'active' : ''}
          onClick={() => setTab('pipelet')}
        >
          Pipelet Developer
        </button>
      </nav>
      <main>
        {tab === 'pipeline' ? (
          <PipelineGuidePanel models={models} modelsNote={note} defaultKey={defaultKey} />
        ) : (
          <CodeGeneratorPanel
            models={models}
            codeModels={codeModels}
            defaultKey={
              codeModels.find((m) => m.id.includes('qwen2.5-coder:1.5b'))
                ? modelKey(codeModels.find((m) => m.id.includes('qwen2.5-coder:1.5b'))!)
                : defaultKey
            }
          />
        )}
      </main>
    </div>
  )
}
