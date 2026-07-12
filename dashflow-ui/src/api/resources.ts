import { apiFetch } from './apiClient'
import type {
  BillingPeriodsResponse,
  CompletenessResponse,
  ConnectorType,
  CreateConnectorRequest,
  CreatePipelineRequest,
  CreateTenantRequest,
  CreateTenantServiceRequest,
  DryRunResponse,
  ErrorSummaryResponse,
  ExecutionLogsResponse,
  HeartbeatResponse,
  LatencyResponse,
  ObservabilityPortalLinks,
  PipelineExecutionDetail,
  PipelineExecutionSummary,
  PipelineBundle,
  PipelineBundleImportRequest,
  PipelineBundleImportResult,
  PipelineResponse,
  PipelineRunResponse,
  QuotaStatusResponse,
  ReplacePipelineStepsRequest,
  ServiceType,
  Tenant,
  TenantConnector,
  TenantService,
  UpdateConnectorRequest,
  UpdatePipelineRequest,
  UpdateTenantRequest,
  UpdateTenantServiceRequest,
  UsageEventsPageResponse,
  UsageSummaryResponse,
} from './types'
import { ApiError } from './types'

async function readJson<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let body: unknown = null
    const text = await res.text()
    try {
      body = text ? JSON.parse(text) : null
    } catch {
      body = text
    }
    const message =
      body && typeof body === 'object' && 'message' in body
        ? String((body as { message: unknown }).message)
        : text || `HTTP ${res.status}`
    throw new ApiError(res.status, body, message)
  }
  return res.json() as Promise<T>
}

export function listConnectors(tenantId: string) {
  return apiFetch('/api/v1/connectors', tenantId).then((r) =>
    readJson<TenantConnector[]>(r),
  )
}

export function getConnector(tenantId: string, id: string) {
  return apiFetch(`/api/v1/connectors/${id}`, tenantId).then((r) =>
    readJson<TenantConnector>(r),
  )
}

export function createConnector(tenantId: string, body: CreateConnectorRequest) {
  return apiFetch('/api/v1/connectors', tenantId, {
    method: 'POST',
    body: JSON.stringify(body),
  }).then((r) => readJson<TenantConnector>(r))
}

export function updateConnector(
  tenantId: string,
  id: string,
  body: UpdateConnectorRequest,
) {
  return apiFetch(`/api/v1/connectors/${id}`, tenantId, {
    method: 'PUT',
    body: JSON.stringify(body),
  }).then((r) => readJson<TenantConnector>(r))
}

export function listConnectorTypes(tenantId: string) {
  return apiFetch('/api/v1/connector-types', tenantId).then((r) =>
    readJson<ConnectorType[]>(r),
  )
}

export function listServices(tenantId: string) {
  return apiFetch('/api/v1/services', tenantId).then((r) =>
    readJson<TenantService[]>(r),
  )
}

export function getService(tenantId: string, id: string) {
  return apiFetch(`/api/v1/services/${id}`, tenantId).then((r) =>
    readJson<TenantService>(r),
  )
}

export function createService(tenantId: string, body: CreateTenantServiceRequest) {
  return apiFetch('/api/v1/services', tenantId, {
    method: 'POST',
    body: JSON.stringify(body),
  }).then((r) => readJson<TenantService>(r))
}

export function updateService(
  tenantId: string,
  id: string,
  body: UpdateTenantServiceRequest,
) {
  return apiFetch(`/api/v1/services/${id}`, tenantId, {
    method: 'PUT',
    body: JSON.stringify(body),
  }).then((r) => readJson<TenantService>(r))
}

export function listServiceTypes(tenantId: string) {
  return apiFetch('/api/v1/service-types', tenantId).then((r) =>
    readJson<ServiceType[]>(r),
  )
}

export function createPipeline(tenantId: string, body: CreatePipelineRequest) {
  return apiFetch('/api/v1/pipelines', tenantId, {
    method: 'POST',
    body: JSON.stringify(body),
  }).then((r) => readJson<PipelineResponse>(r))
}

export function getPipeline(tenantId: string, pipelineId: string) {
  return apiFetch(`/api/v1/pipelines/${pipelineId}`, tenantId).then((r) =>
    readJson<PipelineResponse>(r),
  )
}

export function updatePipeline(
  tenantId: string,
  pipelineId: string,
  body: UpdatePipelineRequest,
) {
  return apiFetch(`/api/v1/pipelines/${pipelineId}`, tenantId, {
    method: 'PUT',
    body: JSON.stringify(body),
  }).then((r) => readJson<PipelineResponse>(r))
}

export function archivePipeline(tenantId: string, pipelineId: string) {
  return apiFetch(`/api/v1/pipelines/${pipelineId}`, tenantId, {
    method: 'DELETE',
  }).then((r) => readJson<PipelineResponse>(r))
}

export function replacePipelineSteps(
  tenantId: string,
  pipelineId: string,
  body: ReplacePipelineStepsRequest,
) {
  return apiFetch(`/api/v1/pipelines/${pipelineId}/steps`, tenantId, {
    method: 'PUT',
    body: JSON.stringify(body),
  }).then((r) => readJson<PipelineResponse>(r))
}

export function listPipelines(tenantId: string) {
  return apiFetch('/api/v1/pipelines', tenantId).then((r) =>
    readJson<PipelineResponse[]>(r),
  )
}

export function exportPipeline(tenantId: string, pipelineId: string) {
  return apiFetch(`/api/v1/pipelines/${pipelineId}/export`, tenantId).then((r) =>
    readJson<PipelineBundle>(r),
  )
}

export function importPipeline(
  tenantId: string,
  body: PipelineBundleImportRequest,
) {
  return apiFetch('/api/v1/pipelines/import', tenantId, {
    method: 'POST',
    body: JSON.stringify(body),
  }).then((r) => readJson<PipelineBundleImportResult>(r))
}

export function runPipeline(tenantId: string, pipelineId: string) {
  return apiFetch(`/api/v1/pipelines/${pipelineId}/run`, tenantId, {
    method: 'POST',
  }).then((r) => readJson<PipelineRunResponse>(r))
}

export function dryRunPipeline(tenantId: string, pipelineId: string) {
  return apiFetch(`/api/v1/pipelines/${pipelineId}/dry-run`, tenantId, {
    method: 'POST',
  }).then((r) => readJson<DryRunResponse>(r))
}

export function listPipelineExecutions(tenantId: string, pipelineId: string) {
  return apiFetch(
    `/api/v1/pipelines/${pipelineId}/executions`,
    tenantId,
  ).then((r) => readJson<PipelineExecutionSummary[]>(r))
}

export function getPipelineExecution(
  tenantId: string,
  pipelineId: string,
  executionId: string,
) {
  return apiFetch(
    `/api/v1/pipelines/${pipelineId}/executions/${executionId}`,
    tenantId,
  ).then((r) => readJson<PipelineExecutionDetail>(r))
}

export function cancelPipelineExecution(
  tenantId: string,
  pipelineId: string,
  executionId: string,
) {
  return apiFetch(
    `/api/v1/pipelines/${pipelineId}/executions/${executionId}/cancel`,
    tenantId,
    { method: 'POST' },
  ).then((r) => readJson<PipelineExecutionDetail>(r))
}

export function getExecutionLogs(tenantId: string, executionId: string) {
  return apiFetch(
    `/api/v1/observability/executions/${executionId}/logs`,
    tenantId,
  ).then((r) => readJson<ExecutionLogsResponse>(r))
}

export function getCompleteness(tenantId: string, pipelineId: string) {
  return apiFetch(
    `/api/v1/observability/pipelines/${pipelineId}/completeness`,
    tenantId,
  ).then((r) => readJson<CompletenessResponse>(r))
}

export function getLatency(tenantId: string, pipelineId: string) {
  return apiFetch(
    `/api/v1/observability/pipelines/${pipelineId}/latency`,
    tenantId,
  ).then((r) => readJson<LatencyResponse>(r))
}

export function getHeartbeat(tenantId: string, pipelineId: string) {
  return apiFetch(
    `/api/v1/observability/pipelines/${pipelineId}/heartbeat`,
    tenantId,
  ).then((r) => readJson<HeartbeatResponse>(r))
}

export function getPipelineErrors(tenantId: string, pipelineId: string) {
  return apiFetch(
    `/api/v1/observability/pipelines/${pipelineId}/errors`,
    tenantId,
  ).then((r) => readJson<ErrorSummaryResponse>(r))
}

export function getObservabilityLinks(
  tenantId: string,
  opts?: { pipelineId?: string; executionId?: string },
) {
  const params = new URLSearchParams()
  if (opts?.pipelineId) {
    params.set('pipelineId', opts.pipelineId)
  }
  if (opts?.executionId) {
    params.set('executionId', opts.executionId)
  }
  const qs = params.toString()
  return apiFetch(
    `/api/v1/observability/links${qs ? `?${qs}` : ''}`,
    tenantId,
  ).then((r) => readJson<ObservabilityPortalLinks>(r))
}

export function getUsageSummary(tenantId: string, period = 'current') {
  return apiFetch(
    `/api/v1/tenants/${tenantId}/usage?period=${encodeURIComponent(period)}`,
    tenantId,
  ).then((r) => readJson<UsageSummaryResponse>(r))
}

export function getUsageEvents(tenantId: string, page = 0, size = 20) {
  return apiFetch(
    `/api/v1/tenants/${tenantId}/usage/events?page=${page}&size=${size}`,
    tenantId,
  ).then((r) => readJson<UsageEventsPageResponse>(r))
}

export function getQuotaStatus(tenantId: string) {
  return apiFetch(`/api/v1/tenants/${tenantId}/quota`, tenantId).then((r) =>
    readJson<QuotaStatusResponse>(r),
  )
}

export function getBillingPeriods(tenantId: string) {
  return apiFetch(`/api/v1/tenants/${tenantId}/billing/periods`, tenantId).then(
    (r) => readJson<BillingPeriodsResponse>(r),
  )
}

/** Platform tenant catalog — header tenant is only for stub auth context. */
export function listTenants(headerTenantId: string) {
  return apiFetch('/api/v1/tenants', headerTenantId).then((r) =>
    readJson<Tenant[]>(r),
  )
}

export function getTenant(headerTenantId: string, id: string) {
  return apiFetch(`/api/v1/tenants/${id}`, headerTenantId).then((r) =>
    readJson<Tenant>(r),
  )
}

export function createTenant(
  headerTenantId: string,
  body: CreateTenantRequest,
) {
  return apiFetch('/api/v1/tenants', headerTenantId, {
    method: 'POST',
    body: JSON.stringify(body),
  }).then((r) => readJson<Tenant>(r))
}

export function updateTenant(
  headerTenantId: string,
  id: string,
  body: UpdateTenantRequest,
) {
  return apiFetch(`/api/v1/tenants/${id}`, headerTenantId, {
    method: 'PUT',
    body: JSON.stringify(body),
  }).then((r) => readJson<Tenant>(r))
}
