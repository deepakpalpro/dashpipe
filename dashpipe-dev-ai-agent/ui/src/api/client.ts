const base = import.meta.env.VITE_API_URL ?? 'http://localhost:8090'

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || response.statusText)
  }
  return response.json() as Promise<T>
}

export async function fetchHealth() {
  return readJson<{ status: string }>(await fetch(`${base}/health`))
}

export async function fetchModels() {
  return readJson<import('./types').AgentModelsResponse>(
    await fetch(`${base}/api/v1/models`),
  )
}

export async function pipelineGuide(body: import('./types').PipelineGuideRequest) {
  return readJson<import('./types').PipelineGuideResponse>(
    await fetch(`${base}/api/v1/pipeline-guide`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }),
  )
}

export async function pythonDeveloper(body: import('./types').PythonDeveloperRequest) {
  return readJson<import('./types').PythonDeveloperResponse>(
    await fetch(`${base}/api/v1/python-developer`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }),
  )
}
