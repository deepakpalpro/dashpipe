export type AgentModelOption = {
  provider: string
  id: string
  label: string
  configured: boolean
  hint?: string | null
}

export type AgentModelsResponse = {
  default_provider: string
  default_model: string
  models: AgentModelOption[]
  note?: string | null
}

export type PipelineGuideRequest = {
  message: string
  provider?: string
  model?: string
  api_key?: string
  catalog?: { id: string; name?: string; category?: string }[]
  current_steps?: { pipelet_id: string; step_order: number }[]
}

export type ProposedStep = {
  pipelet_id: string
  step_order: number
  execution_config?: Record<string, unknown>
  deployment_config?: Record<string, unknown>
}

export type ProposedPipeline = {
  name: string
  description?: string
  steps: ProposedStep[]
}

export type PipelineGuideResponse = {
  reply: string
  thinking?: string | null
  proposed_pipeline?: ProposedPipeline | null
  model?: string | null
  raw_model_output?: string | null
}

export type PythonDeveloperRequest = {
  requirement: string
  context?: string
  provider?: string
  model?: string
  api_key?: string
}

export type GeneratedFile = { path: string; content: string }

export type PythonDeveloperResponse = {
  model: string
  model_justification: string
  thinking: string
  plain_text: string
  files: GeneratedFile[]
}
