export type ConnectorInstanceStatus = 'ACTIVE' | 'INACTIVE' | 'ERROR'

export type TenantConnector = {
  id: string
  tenantId: string
  connectorTypeId: string
  name: string
  config: Record<string, unknown>
  deployment_config?: Record<string, unknown> | null
  execution_config?: Record<string, unknown> | null
  status: ConnectorInstanceStatus
  lastTestedAt: string | null
  createdAt: string
}

export type CreateConnectorRequest = {
  connectorTypeId: string
  name: string
  config: Record<string, unknown>
  deployment_config?: Record<string, unknown>
  execution_config?: Record<string, unknown>
}

export type ConnectorType = {
  id: string
  type: string
  displayName: string
  configSchema: Record<string, unknown> | null
  spiClass: string
  spiVersion: string
}

export type ServiceInstanceStatus = 'ACTIVE' | 'INACTIVE' | 'ERROR'

export type TenantService = {
  id: string
  tenantId: string
  serviceTypeId: string
  vendor: string
  name: string
  config: Record<string, unknown>
  deployment_config?: Record<string, unknown> | null
  execution_config?: Record<string, unknown> | null
  inheritsDefault: boolean
  status: ServiceInstanceStatus
  createdAt: string
}

export type CreateTenantServiceRequest = {
  serviceTypeId: string
  vendor: string
  name: string
  tenantConfig: Record<string, unknown>
  deployment_config?: Record<string, unknown>
  execution_config?: Record<string, unknown>
  inheritsDefault?: boolean
}

export type UpdateTenantServiceRequest = {
  name: string
  tenantConfig?: Record<string, unknown>
  deployment_config?: Record<string, unknown>
  execution_config?: Record<string, unknown>
  inheritsDefault?: boolean
  status?: ServiceInstanceStatus
}

export type ServiceDefault = {
  id: string
  vendor: string
  baseServiceClass?: string | null
  defaultConfig?: Record<string, unknown> | null
  configSchema?: Record<string, unknown> | null
}

export type ServiceType = {
  id: string
  type: string
  displayName: string
  defaults?: ServiceDefault[]
}

export type UpdateConnectorRequest = {
  name: string
  config?: Record<string, unknown>
  deployment_config?: Record<string, unknown>
  execution_config?: Record<string, unknown>
  status?: ConnectorInstanceStatus
}

export type CreatePipelineRequest = {
  name: string
  description?: string
  visibility?: 'PRIVATE' | 'TENANT'
  executionMode?: 'ASYNC' | 'SYNC'
  deployment_config?: Record<string, unknown>
  execution_config?: Record<string, unknown>
}

export type UpdatePipelineRequest = {
  name: string
  description?: string | null
  visibility?: 'PRIVATE' | 'TENANT'
  executionMode?: 'ASYNC' | 'SYNC'
  status?: string
  deployment_config?: Record<string, unknown>
  execution_config?: Record<string, unknown>
}

export type PipelineStepPayload = {
  pipelet_id: string
  step_order: number
  /** Legacy alias of execution_config. */
  config: Record<string, unknown>
  deployment_config?: Record<string, unknown>
  execution_config?: Record<string, unknown>
  connector_ids: string[]
  service_ids: string[]
  input_queue: string | null
  output_queue: string | null
}

export type ReplacePipelineStepsRequest = {
  steps: PipelineStepPayload[]
}

export type PipelineStepResponse = {
  id?: string
  pipelet_id: string
  step_order: number
  config?: Record<string, unknown> | null
  deployment_config?: Record<string, unknown> | null
  execution_config?: Record<string, unknown> | null
  connector_ids?: string[]
  service_ids?: string[]
  input_queue?: string | null
  output_queue?: string | null
}

export type PipelineResponse = {
  id: string
  tenantId: string
  name: string
  description?: string | null
  visibility?: string
  execution_mode?: string
  version: number
  status: string
  deployment_config?: Record<string, unknown> | null
  execution_config?: Record<string, unknown> | null
  created_at?: string
  updated_at?: string
  steps?: PipelineStepResponse[]
}

export type PipelineBundle = {
  format_version: string
  exported_at?: string
  pipeline: {
    name: string
    description?: string | null
    visibility?: string
    execution_mode?: string
    deployment_config?: Record<string, unknown> | null
    execution_config?: Record<string, unknown> | null
  }
  steps: Array<{
    pipelet_id: string
    step_order: number
    deployment_config?: Record<string, unknown> | null
    execution_config?: Record<string, unknown> | null
    connector_refs?: string[]
    service_refs?: string[]
    input_queue?: string | null
    output_queue?: string | null
    resource_limits?: Record<string, unknown> | null
  }>
  connectors: Array<{
    export_key: string
    connectorTypeId: string
    name: string
    deployment_config?: Record<string, unknown> | null
    execution_config?: Record<string, unknown> | null
  }>
  services: Array<{
    export_key: string
    serviceTypeId: string
    vendor: string
    name: string
    inheritsDefault?: boolean
    deployment_config?: Record<string, unknown> | null
    execution_config?: Record<string, unknown> | null
  }>
}

export type PipelineBundleImportRequest = {
  bundle: PipelineBundle
  name?: string
  conflict_strategy?: 'create' | 'reuse'
}

export type PipelineBundleImportResult = {
  pipeline_id: string
  name: string
  created_connectors: string[]
  reused_connectors: string[]
  created_services: string[]
  reused_services: string[]
  warnings: string[]
}

export type PipelineRunResponse = {
  execution_id: string
  status: string
  pipeline_id: string
  started_at?: string
}

export type ExecutionStepStatusDto = {
  step_order: number
  node_id?: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
}

export type PipelineExecutionSummary = {
  id: string
  pipeline_id: string
  tenant_id?: string
  pipeline_version?: number
  status: string
  trigger?: string
  started_at?: string
  completed_at?: string | null
  records_in?: number
  records_out?: number
  completeness_pct?: number | null
}

export type PipelineExecutionDetail = PipelineExecutionSummary & {
  steps?: ExecutionStepStatusDto[]
  error_summary?: string | null
}

export type ExecutionLogEntry = {
  '@timestamp'?: string
  level?: string
  pipelet_id?: string
  pod_name?: string
  message?: string
  records_in?: number | null
  records_out?: number | null
  duration_ms?: number | null
}

export type ExecutionLogsResponse = {
  execution_id: string
  tenant_id: string
  pipeline_id: string
  logs: ExecutionLogEntry[]
}

export type DryRunResponse = {
  valid: boolean
  messages: string[]
}

export type QuotaBlockedBody = {
  error: string
  code: string
  message: string
  credit_balance?: number
  breached_dimension?: string
}

export class ApiError extends Error {
  status: number
  body: unknown

  constructor(status: number, body: unknown, message?: string) {
    super(message ?? `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

export type CompletenessResponse = {
  pipeline_id: string
  tenant_id: string
  execution_id: string
  records_in: number
  records_out: number
  completeness_pct: number
  completeness_ratio: number
}

export type LatencyResponse = {
  pipeline_id: string
  tenant_id: string
  sample_count: number
  mean_ms: number
  max_ms: number
  p50_ms?: number
  p95_ms?: number
  p99_ms?: number
}

export type HeartbeatResponse = {
  pipeline_id: string
  tenant_id: string
  last_heartbeat_epoch_seconds: number | null
  stale: boolean
}

export type ErrorTypeCount = {
  error_type: string
  count: number
}

export type ErrorSummaryResponse = {
  pipeline_id: string
  tenant_id: string
  total_errors: number
  by_type: ErrorTypeCount[]
}

export type ObservabilityPortalLinks = {
  grafana_enabled: boolean
  grafana_url: string | null
  grafana_label: string
  elasticsearch_enabled: boolean
  elasticsearch_url: string | null
  elasticsearch_label: string
}

export type TimeRange = '1h' | '24h' | '7d'

export type DimensionUsage = {
  quantity: number | string
  cost: number | string
}

export type UsageSummaryResponse = {
  tenant_id: string
  period_start: string
  period_end: string
  dimensions: Record<string, DimensionUsage>
  total_cost: number | string
  credit_balance: number | string
}

export type UsageEventItem = {
  id: string
  dimension: string
  quantity: number | string
  unit: string
  execution_id?: string | null
  pipeline_id?: string | null
  pipelet_id?: string | null
  connector_id?: string | null
  recorded_at: string
  idempotency_key?: string | null
}

export type UsageEventsPageResponse = {
  tenant_id: string
  items: UsageEventItem[]
  page: number
  size: number
  total_elements: number
  total_pages: number
}

export type DimensionQuotaStatus = {
  soft: number | string | null
  hard: number | string | null
  usage: number | string | null
}

export type QuotaStatusResponse = {
  tenant_id: string
  decision: string
  message: string
  allowed: boolean
  credit_balance: number | string
  breached_dimension?: string | null
  soft_limit?: number | string | null
  hard_limit?: number | string | null
  current_usage?: number | string | null
  dimensions?: Record<string, DimensionQuotaStatus>
}

export type BillingPeriodItem = {
  id: string
  period_start: string
  period_end: string
  total_cost: number | string
  status: string
}

export type BillingPeriodsResponse = {
  tenant_id: string
  periods: BillingPeriodItem[]
}

export type TenantStatus = 'active' | 'suspended' | 'trial'

export type Tenant = {
  id: string
  name: string
  slug: string
  status: TenantStatus
  creditBalance: number | string
  createdAt: string
  updatedAt: string
}

export type CreateTenantRequest = {
  name: string
  slug: string
  status?: TenantStatus
}

export type UpdateTenantRequest = {
  name: string
  status?: TenantStatus
}
