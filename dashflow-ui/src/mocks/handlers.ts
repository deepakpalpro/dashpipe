import { http, HttpResponse } from 'msw'
import { isSecretKey, REDACTED, redactConfig } from '../api/secrets'
import seedAuthServices from '../fixtures/seed-auth-services.json'
import seedConnectors from '../fixtures/seed-connectors.json'
import type {
  ConnectorType,
  CreateConnectorRequest,
  CreatePipelineRequest,
  CreateTenantRequest,
  CreateTenantServiceRequest,
  PipelineExecutionDetail,
  PipelineResponse,
  ReplacePipelineStepsRequest,
  ServiceType,
  Tenant,
  TenantConnector,
  TenantService,
  UpdateConnectorRequest,
  UpdatePipelineRequest,
  UpdateTenantRequest,
  UpdateTenantServiceRequest,
} from '../api/types'

function tenantId(request: Request): string {
  return request.headers.get('X-Tenant-Id') ?? 'T001'
}

function nowIso() {
  return new Date().toISOString()
}

function id(prefix: string) {
  return `${prefix}-${crypto.randomUUID().slice(0, 8)}`
}

function mergePreservingSecrets(
  existing: Record<string, unknown>,
  incoming: Record<string, unknown>,
): Record<string, unknown> {
  const out = { ...existing }
  for (const [key, value] of Object.entries(incoming)) {
    if (
      isSecretKey(key) &&
      (value === REDACTED || value === '***' || value === '••••••') &&
      key in existing
    ) {
      continue
    }
    out[key] = value
  }
  return out
}

function asConnectorResponse(row: TenantConnector): TenantConnector {
  const execution = row.execution_config ?? row.config ?? {}
  const deployment = row.deployment_config ?? {}
  const redactedExecution = redactConfig(execution)
  return {
    ...row,
    config: redactedExecution,
    execution_config: redactedExecution,
    deployment_config: redactConfig(deployment),
  }
}

function asServiceResponse(row: TenantService): TenantService {
  const execution = row.execution_config ?? row.config ?? {}
  const deployment = row.deployment_config ?? {}
  const redactedExecution = redactConfig(execution)
  return {
    ...row,
    config: redactedExecution,
    execution_config: redactedExecution,
    deployment_config: redactConfig(deployment),
  }
}

export const CONNECTOR_TYPES: ConnectorType[] = [
  {
    id: 'ct-rest',
    type: 'REST',
    displayName: 'REST / HTTP',
    configSchema: {
      type: 'object',
      required: ['baseUrl'],
      properties: {
        baseUrl: { type: 'string' },
        api_key: { type: 'string' },
      },
    },
    spiClass: 'com.dashflow.connector.rest.RestConnector',
    spiVersion: '1.0',
  },
  {
    id: 'ct-event-listener',
    type: 'EVENT_LISTENER',
    displayName: 'Event Listener / Webhook',
    configSchema: {
      type: 'object',
      required: ['path'],
      properties: {
        path: { type: 'string' },
        secret: { type: 'string' },
      },
    },
    spiClass: 'com.dashflow.connector.spi.Connector',
    spiVersion: '1.0',
  },
  {
    id: 'ct-storage',
    type: 'STORAGE',
    displayName: 'Object Storage (S3)',
    configSchema: {
      type: 'object',
      required: ['bucket'],
      properties: {
        bucket: { type: 'string' },
        region: { type: 'string' },
        prefix: { type: 'string' },
      },
    },
    spiClass: 'com.dashflow.connector.storage.StorageConnector',
    spiVersion: '1.0',
  },
  {
    id: 'ct-message-bus',
    type: 'MESSAGE_BUS',
    displayName: 'Message Bus (SQS)',
    configSchema: {
      type: 'object',
      required: ['queueUrl'],
      properties: {
        queueUrl: { type: 'string' },
        region: { type: 'string' },
      },
    },
    spiClass: 'com.dashflow.connector.messagebus.MessageBusConnector',
    spiVersion: '1.0',
  },
]

export const SERVICE_TYPES: ServiceType[] = [
  {
    id: 'st-auth',
    type: 'AUTH',
    displayName: 'Authentication',
    defaults: [
      { id: 'sd-auth-stub', vendor: 'StubAuth' },
      { id: 'sd-auth-oauth', vendor: 'OAuth' },
      { id: 'sd-auth-oidc', vendor: 'OIDC' },
      { id: 'sd-auth-keycloak', vendor: 'Keycloak' },
      { id: 'sd-auth-aad', vendor: 'AAD' },
      { id: 'sd-auth-cognito', vendor: 'AWSCognito' },
      { id: 'sd-auth-azure-mi', vendor: 'AzureMI' },
      { id: 'sd-auth-cert', vendor: 'CertBased' },
      { id: 'sd-auth-jwt', vendor: 'JWT' },
    ],
  },
]

/** Mutable fixture store — reset between tests via `resetMockDb()`. */
export const mockDb = {
  tenants: [] as Tenant[],
  connectors: [] as TenantConnector[],
  services: [] as TenantService[],
  pipelines: [] as PipelineResponse[],
  /** Last PUT steps body — asserted by save tests. */
  lastStepsPut: null as ReplacePipelineStepsRequest | null,
  executions: {} as Record<string, PipelineExecutionDetail>,
  executionPollCount: {} as Record<string, number>,
  /** When true, POST .../run returns 402. */
  blockRunsWith402: false,
}

export function resetMockDb() {
  mockDb.tenants = [
    {
      id: 'T001',
      name: 'Acme Analytics',
      slug: 'acme-analytics',
      status: 'active',
      creditBalance: 100,
      createdAt: '2026-07-01T00:00:00Z',
      updatedAt: '2026-07-01T00:00:00Z',
    },
    {
      id: 'T002',
      name: 'Beta Logistics',
      slug: 'beta-logistics',
      status: 'active',
      creditBalance: 100,
      createdAt: '2026-07-01T00:00:00Z',
      updatedAt: '2026-07-01T00:00:00Z',
    },
  ]
  mockDb.pipelines = [
    {
      id: 'pipe-demo',
      tenantId: 'T001',
      name: 'threeStage',
      description: 'Demo pipeline',
      visibility: 'PRIVATE',
      execution_mode: 'ASYNC',
      version: 1,
      status: 'ACTIVE',
      deployment_config: {
        cloud: 'aws',
        region: 'us-east-1',
        accessKey: 'AKIA••••',
      },
      execution_config: { timeoutMs: 30000 },
      created_at: '2026-07-01T00:00:00Z',
      updated_at: '2026-07-01T00:00:00Z',
      steps: [
        {
          id: 'step-1',
          pipelet_id: 'plet-rest-source',
          step_order: 1,
          config: {},
          deployment_config: { cloud: 'aws', region: 'us-east-1' },
          execution_config: {},
          connector_ids: ['conn-plet-rest-source'],
          service_ids: [],
          input_queue: null,
          output_queue: 'q-stage-1',
        },
        {
          id: 'step-2',
          pipelet_id: 'plet-json-transform',
          step_order: 2,
          config: { mode: 'flatten' },
          deployment_config: { cloud: 'aws', region: 'us-east-1' },
          execution_config: { mode: 'flatten' },
          connector_ids: [],
          service_ids: [],
          input_queue: 'q-stage-1',
          output_queue: 'q-stage-2',
        },
        {
          id: 'step-3',
          pipelet_id: 'plet-s3-destination',
          step_order: 3,
          config: {},
          deployment_config: { cloud: 'aws', region: 'us-east-1' },
          execution_config: {},
          connector_ids: [],
          service_ids: ['svc-auth-oauth'],
          input_queue: 'q-stage-2',
          output_queue: null,
        },
      ],
    },
    {
      id: 'pipe-draft',
      tenantId: 'T001',
      name: 'draftOnly',
      description: null,
      visibility: 'PRIVATE',
      execution_mode: 'ASYNC',
      version: 1,
      status: 'DRAFT',
      deployment_config: {},
      execution_config: {},
      created_at: '2026-07-02T00:00:00Z',
      updated_at: '2026-07-02T00:00:00Z',
      steps: [],
    },
  ]
  mockDb.lastStepsPut = null
  mockDb.executions = {}
  mockDb.executionPollCount = {}
  mockDb.blockRunsWith402 = false
  mockDb.connectors = [
    {
      id: 'conn-1',
      tenantId: 'T001',
      connectorTypeId: 'ct-rest',
      name: 'Orders API',
      config: {
        baseUrl: 'https://orders.example.com',
        api_key: 'super-secret-key',
      },
      deployment_config: { cloud: 'aws', region: 'us-east-1' },
      execution_config: {
        baseUrl: 'https://orders.example.com',
        api_key: 'super-secret-key',
      },
      status: 'ACTIVE',
      lastTestedAt: null,
      createdAt: '2026-07-01T00:00:00Z',
    },
    ...(seedConnectors as TenantConnector[]),
  ]
  mockDb.services = [
    {
      id: 'svc-1',
      tenantId: 'T001',
      serviceTypeId: 'st-auth',
      vendor: 'StubAuth',
      name: 'Primary Auth',
      config: {
        client_id: 'public-client',
        client_secret: 'raw-secret-must-not-leak',
      },
      deployment_config: { cloud: 'aws', region: 'us-east-1' },
      execution_config: {
        client_id: 'public-client',
        client_secret: 'raw-secret-must-not-leak',
      },
      inheritsDefault: false,
      status: 'ACTIVE',
      createdAt: '2026-07-01T00:00:00Z',
    },
    ...(seedAuthServices as TenantService[]),
  ]
}

resetMockDb()

export const connectorHandlers = [
  http.get('/api/v1/connector-types', () =>
    HttpResponse.json(CONNECTOR_TYPES),
  ),

  http.get('/api/v1/connectors', ({ request }) => {
    const tid = tenantId(request)
    return HttpResponse.json(
      mockDb.connectors
        .filter((c) => c.tenantId === tid)
        .map(asConnectorResponse),
    )
  }),

  http.get('/api/v1/connectors/:id', ({ params, request }) => {
    const tid = tenantId(request)
    const row = mockDb.connectors.find(
      (c) => c.id === params.id && c.tenantId === tid,
    )
    if (!row) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    return HttpResponse.json(asConnectorResponse(row))
  }),

  http.post('/api/v1/connectors', async ({ request }) => {
    const tid = tenantId(request)
    const body = (await request.json()) as CreateConnectorRequest
    if (!body.connectorTypeId || !body.name?.trim()) {
      return HttpResponse.json({ message: 'Validation failed' }, { status: 400 })
    }
    const execution = {
      ...(body.execution_config ?? body.config ?? {}),
    }
    const created: TenantConnector = {
      id: id('conn'),
      tenantId: tid,
      connectorTypeId: body.connectorTypeId,
      name: body.name.trim(),
      config: execution,
      deployment_config: { ...(body.deployment_config ?? {}) },
      execution_config: execution,
      status: 'ACTIVE',
      lastTestedAt: null,
      createdAt: nowIso(),
    }
    mockDb.connectors.push(created)
    return HttpResponse.json(asConnectorResponse(created), { status: 201 })
  }),

  http.put('/api/v1/connectors/:id', async ({ params, request }) => {
    const tid = tenantId(request)
    const idx = mockDb.connectors.findIndex(
      (c) => c.id === params.id && c.tenantId === tid,
    )
    if (idx < 0) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    const body = (await request.json()) as UpdateConnectorRequest
    const prev = mockDb.connectors[idx]
    const incomingExecution = body.execution_config ?? body.config
    const execution = incomingExecution
      ? mergePreservingSecrets(
          prev.execution_config ?? prev.config,
          incomingExecution,
        )
      : (prev.execution_config ?? prev.config)
    const deployment =
      body.deployment_config != null
        ? mergePreservingSecrets(
            prev.deployment_config ?? {},
            body.deployment_config,
          )
        : (prev.deployment_config ?? {})
    const updated: TenantConnector = {
      ...prev,
      name: body.name,
      config: execution,
      execution_config: execution,
      deployment_config: deployment,
      status: body.status ?? prev.status,
    }
    mockDb.connectors[idx] = updated
    return HttpResponse.json(asConnectorResponse(updated))
  }),
]

export const serviceHandlers = [
  http.get('/api/v1/service-types', () => HttpResponse.json(SERVICE_TYPES)),

  http.get('/api/v1/services', ({ request }) => {
    const tid = tenantId(request)
    return HttpResponse.json(
      mockDb.services
        .filter((s) => s.tenantId === tid)
        .map(asServiceResponse),
    )
  }),

  http.get('/api/v1/services/:id', ({ params, request }) => {
    const tid = tenantId(request)
    const row = mockDb.services.find(
      (s) => s.id === params.id && s.tenantId === tid,
    )
    if (!row) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    return HttpResponse.json(asServiceResponse(row))
  }),

  http.post('/api/v1/services', async ({ request }) => {
    const tid = tenantId(request)
    const body = (await request.json()) as CreateTenantServiceRequest
    if (!body.serviceTypeId || !body.vendor?.trim() || !body.name?.trim()) {
      return HttpResponse.json({ message: 'Validation failed' }, { status: 400 })
    }
    const execution = {
      ...(body.execution_config ?? body.tenantConfig ?? {}),
    }
    const created: TenantService = {
      id: id('svc'),
      tenantId: tid,
      serviceTypeId: body.serviceTypeId,
      vendor: body.vendor.trim(),
      name: body.name.trim(),
      config: execution,
      deployment_config: { ...(body.deployment_config ?? {}) },
      execution_config: execution,
      inheritsDefault: body.inheritsDefault ?? false,
      status: 'ACTIVE',
      createdAt: nowIso(),
    }
    mockDb.services.push(created)
    return HttpResponse.json(asServiceResponse(created), { status: 201 })
  }),

  http.put('/api/v1/services/:id', async ({ params, request }) => {
    const tid = tenantId(request)
    const idx = mockDb.services.findIndex(
      (s) => s.id === params.id && s.tenantId === tid,
    )
    if (idx < 0) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    const body = (await request.json()) as UpdateTenantServiceRequest
    const prev = mockDb.services[idx]
    const incomingExecution = body.execution_config ?? body.tenantConfig
    const execution = incomingExecution
      ? mergePreservingSecrets(
          prev.execution_config ?? prev.config,
          incomingExecution,
        )
      : (prev.execution_config ?? prev.config)
    const deployment =
      body.deployment_config != null
        ? mergePreservingSecrets(
            prev.deployment_config ?? {},
            body.deployment_config,
          )
        : (prev.deployment_config ?? {})
    const updated: TenantService = {
      ...prev,
      name: body.name,
      config: execution,
      execution_config: execution,
      deployment_config: deployment,
      inheritsDefault: body.inheritsDefault ?? prev.inheritsDefault,
      status: body.status ?? prev.status,
    }
    mockDb.services[idx] = updated
    return HttpResponse.json(asServiceResponse(updated))
  }),
]

export const pipeletHandlers = [
  http.get('/api/v1/pipelets', async () => {
    const { getPipeletCatalog } = await import('../features/pipelets/fixture')
    return HttpResponse.json(getPipeletCatalog())
  }),

  http.post('/api/v1/pipelets/register', async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json(
      { id: `plet-registered-${Date.now()}`, ...(body as object) },
      { status: 201 },
    )
  }),
]

export const pipelineHandlers = [
  http.get('/api/v1/pipelines', ({ request }) => {
    const tid = tenantId(request)
    return HttpResponse.json(
      mockDb.pipelines
        .filter(
          (p) => p.tenantId === tid && p.status.toUpperCase() !== 'ARCHIVED',
        )
        .map((p) => ({
          ...p,
          deployment_config: redactConfig(
            (p.deployment_config as Record<string, unknown>) ?? {},
          ),
          execution_config: redactConfig(
            (p.execution_config as Record<string, unknown>) ?? {},
          ),
        })),
    )
  }),

  http.get('/api/v1/pipelines/:id/export', ({ params, request }) => {
    const tid = tenantId(request)
    const pipe = mockDb.pipelines.find(
      (p) => p.id === params.id && p.tenantId === tid,
    )
    if (!pipe) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    const steps = pipe.steps ?? []
    const connectorIds = new Set(
      steps.flatMap((s) => s.connector_ids ?? []),
    )
    const serviceIds = new Set(steps.flatMap((s) => s.service_ids ?? []))
    const connectors = mockDb.connectors
      .filter((c) => c.tenantId === tid && connectorIds.has(c.id))
      .map((c) => ({
        export_key: `${c.connectorTypeId}::${c.name}`,
        connectorTypeId: c.connectorTypeId,
        name: c.name,
        deployment_config: c.deployment_config ?? {},
        execution_config: c.execution_config ?? c.config ?? {},
      }))
    const services = mockDb.services
      .filter((s) => s.tenantId === tid && serviceIds.has(s.id))
      .map((s) => ({
        export_key: `${s.serviceTypeId}::${s.vendor}::${s.name}`,
        serviceTypeId: s.serviceTypeId,
        vendor: s.vendor,
        name: s.name,
        inheritsDefault: s.inheritsDefault,
        deployment_config: s.deployment_config ?? {},
        execution_config: s.execution_config ?? s.config ?? {},
      }))
    const connectorKeyById: Record<string, string> = {}
    for (const c of mockDb.connectors.filter((x) => x.tenantId === tid)) {
      connectorKeyById[c.id] = `${c.connectorTypeId}::${c.name}`
    }
    const serviceKeyById: Record<string, string> = {}
    for (const s of mockDb.services.filter((x) => x.tenantId === tid)) {
      serviceKeyById[s.id] = `${s.serviceTypeId}::${s.vendor}::${s.name}`
    }
    return HttpResponse.json({
      format_version: '1',
      exported_at: nowIso(),
      pipeline: {
        name: pipe.name,
        description: pipe.description ?? null,
        visibility: pipe.visibility ?? 'private',
        execution_mode: pipe.execution_mode ?? 'async',
        deployment_config: pipe.deployment_config ?? {},
        execution_config: pipe.execution_config ?? {},
      },
      steps: steps.map((s) => ({
        pipelet_id: s.pipelet_id,
        step_order: s.step_order,
        deployment_config: s.deployment_config ?? {},
        execution_config: s.execution_config ?? s.config ?? {},
        connector_refs: (s.connector_ids ?? [])
          .map((cid) => connectorKeyById[cid])
          .filter(Boolean),
        service_refs: (s.service_ids ?? [])
          .map((sid) => serviceKeyById[sid])
          .filter(Boolean),
        input_queue: s.input_queue ?? null,
        output_queue: s.output_queue ?? null,
      })),
      connectors,
      services,
    })
  }),

  http.post('/api/v1/pipelines/import', async ({ request }) => {
    const tid = tenantId(request)
    const body = (await request.json()) as {
      bundle?: {
        format_version?: string
        pipeline?: {
          name?: string
          description?: string | null
          visibility?: string
          execution_mode?: string
          deployment_config?: Record<string, unknown>
          execution_config?: Record<string, unknown>
        }
        steps?: Array<{
          pipelet_id: string
          step_order: number
          deployment_config?: Record<string, unknown>
          execution_config?: Record<string, unknown>
          connector_refs?: string[]
          service_refs?: string[]
          input_queue?: string | null
          output_queue?: string | null
        }>
        connectors?: Array<{
          export_key: string
          connectorTypeId: string
          name: string
          deployment_config?: Record<string, unknown>
          execution_config?: Record<string, unknown>
        }>
        services?: Array<{
          export_key: string
          serviceTypeId: string
          vendor: string
          name: string
          inheritsDefault?: boolean
          deployment_config?: Record<string, unknown>
          execution_config?: Record<string, unknown>
        }>
      }
      name?: string
      conflict_strategy?: string
    }
    const bundle = body.bundle
    if (!bundle?.pipeline?.name) {
      return HttpResponse.json({ message: 'bundle.pipeline required' }, { status: 400 })
    }
    const reuse = body.conflict_strategy === 'reuse'
    const warnings: string[] = []
    const createdConnectors: string[] = []
    const reusedConnectors: string[] = []
    const createdServices: string[] = []
    const reusedServices: string[] = []
    const connectorKeyToId: Record<string, string> = {}
    const serviceKeyToId: Record<string, string> = {}

    for (const c of bundle.connectors ?? []) {
      const existing = mockDb.connectors.find(
        (x) =>
          x.tenantId === tid &&
          x.connectorTypeId === c.connectorTypeId &&
          x.name === c.name,
      )
      if (reuse && existing) {
        connectorKeyToId[c.export_key] = existing.id
        reusedConnectors.push(c.export_key)
        continue
      }
      const newId = id('conn')
      mockDb.connectors.push({
        id: newId,
        tenantId: tid,
        connectorTypeId: c.connectorTypeId,
        name: c.name,
        config: c.execution_config ?? {},
        deployment_config: c.deployment_config ?? {},
        execution_config: c.execution_config ?? {},
        status: 'ACTIVE',
        lastTestedAt: null,
        createdAt: nowIso(),
      })
      connectorKeyToId[c.export_key] = newId
      createdConnectors.push(c.export_key)
    }

    for (const s of bundle.services ?? []) {
      const existing = mockDb.services.find(
        (x) =>
          x.tenantId === tid &&
          x.serviceTypeId === s.serviceTypeId &&
          x.vendor === s.vendor &&
          x.name === s.name,
      )
      if (reuse && existing) {
        serviceKeyToId[s.export_key] = existing.id
        reusedServices.push(s.export_key)
        continue
      }
      const newId = id('svc')
      mockDb.services.push({
        id: newId,
        tenantId: tid,
        serviceTypeId: s.serviceTypeId,
        vendor: s.vendor,
        name: s.name,
        config: s.execution_config ?? {},
        deployment_config: s.deployment_config ?? {},
        execution_config: s.execution_config ?? {},
        inheritsDefault: s.inheritsDefault ?? true,
        status: 'ACTIVE',
        createdAt: nowIso(),
      })
      serviceKeyToId[s.export_key] = newId
      createdServices.push(s.export_key)
    }

    let pipeName = (body.name ?? bundle.pipeline.name).trim()
    if (mockDb.pipelines.some((p) => p.tenantId === tid && p.name === pipeName)) {
      pipeName = `${pipeName} (import)`
      warnings.push(`Pipeline renamed to ${pipeName}`)
    }
    const pipeId = id('pipe')
    const steps = (bundle.steps ?? []).map((s) => {
      const execution = s.execution_config ?? {}
      return {
        pipelet_id: s.pipelet_id,
        step_order: s.step_order,
        config: execution,
        deployment_config: s.deployment_config ?? {},
        execution_config: execution,
        connector_ids: (s.connector_refs ?? [])
          .map((k) => connectorKeyToId[k])
          .filter(Boolean) as string[],
        service_ids: (s.service_refs ?? [])
          .map((k) => serviceKeyToId[k])
          .filter(Boolean) as string[],
        input_queue: s.input_queue ?? null,
        output_queue: s.output_queue ?? null,
      }
    })
    const created: PipelineResponse = {
      id: pipeId,
      tenantId: tid,
      name: pipeName,
      description: bundle.pipeline.description ?? null,
      visibility: bundle.pipeline.visibility ?? 'PRIVATE',
      execution_mode: bundle.pipeline.execution_mode ?? 'ASYNC',
      version: 1,
      status: steps.length ? 'ACTIVE' : 'DRAFT',
      deployment_config: bundle.pipeline.deployment_config ?? {},
      execution_config: bundle.pipeline.execution_config ?? {},
      created_at: nowIso(),
      updated_at: nowIso(),
      steps,
    }
    mockDb.pipelines.push(created)
    return HttpResponse.json(
      {
        pipeline_id: pipeId,
        name: pipeName,
        created_connectors: createdConnectors,
        reused_connectors: reusedConnectors,
        created_services: createdServices,
        reused_services: reusedServices,
        warnings,
      },
      { status: 201 },
    )
  }),

  http.get('/api/v1/pipelines/:id', ({ params, request }) => {
    const tid = tenantId(request)
    const pipe = mockDb.pipelines.find(
      (p) => p.id === params.id && p.tenantId === tid,
    )
    if (!pipe) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    return HttpResponse.json({
      ...pipe,
      deployment_config: redactConfig(
        (pipe.deployment_config as Record<string, unknown>) ?? {},
      ),
      execution_config: redactConfig(
        (pipe.execution_config as Record<string, unknown>) ?? {},
      ),
    })
  }),

  http.delete('/api/v1/pipelines/:id', ({ params, request }) => {
    const tid = tenantId(request)
    const idx = mockDb.pipelines.findIndex(
      (p) => p.id === params.id && p.tenantId === tid,
    )
    if (idx < 0) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    const archived: PipelineResponse = {
      ...mockDb.pipelines[idx],
      status: 'ARCHIVED',
      version: mockDb.pipelines[idx].version + 1,
      updated_at: nowIso(),
    }
    mockDb.pipelines[idx] = archived
    return HttpResponse.json(archived)
  }),

  http.post('/api/v1/pipelines', async ({ request }) => {
    const tid = tenantId(request)
    const body = (await request.json()) as CreatePipelineRequest
    if (!body.name?.trim()) {
      return HttpResponse.json({ message: 'Validation failed' }, { status: 400 })
    }
    const created: PipelineResponse = {
      id: id('pipe'),
      tenantId: tid,
      name: body.name.trim(),
      description: body.description ?? null,
      visibility: body.visibility ?? 'PRIVATE',
      execution_mode: body.executionMode ?? 'ASYNC',
      version: 1,
      status: 'DRAFT',
      deployment_config: redactConfig(body.deployment_config ?? {}),
      execution_config: redactConfig(body.execution_config ?? {}),
      created_at: nowIso(),
      updated_at: nowIso(),
      steps: [],
    }
    mockDb.pipelines.push({
      ...created,
      deployment_config: { ...(body.deployment_config ?? {}) },
      execution_config: { ...(body.execution_config ?? {}) },
    })
    return HttpResponse.json(created, { status: 201 })
  }),

  http.put('/api/v1/pipelines/:id', async ({ params, request }) => {
    const tid = tenantId(request)
    const idx = mockDb.pipelines.findIndex(
      (p) => p.id === params.id && p.tenantId === tid,
    )
    if (idx < 0) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    const body = (await request.json()) as UpdatePipelineRequest
    const existing = mockDb.pipelines[idx]
    const mergedDeployment =
      body.deployment_config != null
        ? mergePreservingSecrets(
            (existing.deployment_config as Record<string, unknown>) ?? {},
            body.deployment_config,
          )
        : ((existing.deployment_config as Record<string, unknown>) ?? {})
    const mergedExecution =
      body.execution_config != null
        ? mergePreservingSecrets(
            (existing.execution_config as Record<string, unknown>) ?? {},
            body.execution_config,
          )
        : ((existing.execution_config as Record<string, unknown>) ?? {})
    const updated: PipelineResponse = {
      ...existing,
      name: body.name?.trim() || existing.name,
      description:
        body.description !== undefined ? body.description : existing.description,
      status: body.status ?? existing.status,
      version: existing.version + 1,
      updated_at: nowIso(),
      deployment_config: mergedDeployment,
      execution_config: mergedExecution,
    }
    mockDb.pipelines[idx] = updated
    return HttpResponse.json({
      ...updated,
      deployment_config: redactConfig(mergedDeployment),
      execution_config: redactConfig(mergedExecution),
    })
  }),

  http.put('/api/v1/pipelines/:id/steps', async ({ params, request }) => {
    const tid = tenantId(request)
    const idx = mockDb.pipelines.findIndex(
      (p) => p.id === params.id && p.tenantId === tid,
    )
    if (idx < 0) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    const body = (await request.json()) as ReplacePipelineStepsRequest
    mockDb.lastStepsPut = body
    const steps = body.steps.map((s) => {
      const execution = s.execution_config ?? s.config ?? {}
      return {
        ...s,
        config: execution,
        execution_config: execution,
        deployment_config: s.deployment_config ?? {},
      }
    })
    const updated: PipelineResponse = {
      ...mockDb.pipelines[idx],
      version: mockDb.pipelines[idx].version + 1,
      updated_at: nowIso(),
      steps,
      status: 'ACTIVE',
    }
    mockDb.pipelines[idx] = updated
    return HttpResponse.json(updated)
  }),

  http.post('/api/v1/pipelines/:id/dry-run', ({ params, request }) => {
    const tid = tenantId(request)
    const pipe = mockDb.pipelines.find(
      (p) => p.id === params.id && p.tenantId === tid,
    )
    if (!pipe) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    return HttpResponse.json({
      valid: true,
      messages: ['Dry-run OK (stub)'],
    })
  }),

  http.post('/api/v1/pipelines/:id/run', ({ params, request }) => {
    const tid = tenantId(request)
    const pipe = mockDb.pipelines.find(
      (p) => p.id === params.id && p.tenantId === tid,
    )
    if (!pipe) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    if (mockDb.blockRunsWith402) {
      return HttpResponse.json(
        {
          error: 'payment_required',
          code: 'NO_CREDIT',
          message: 'Credit balance is zero; top up to run pipelines.',
          credit_balance: 0,
        },
        { status: 402 },
      )
    }
    const executionId = id('exec')
    mockDb.executions[executionId] = {
      id: executionId,
      pipeline_id: pipe.id,
      tenant_id: tid,
      pipeline_version: pipe.version,
      status: 'RUNNING',
      trigger: 'manual',
      started_at: nowIso(),
      completed_at: null,
      records_in: 0,
      records_out: 0,
      completeness_pct: null,
      steps: [
        { step_order: 1, status: 'RUNNING' },
        { step_order: 2, status: 'PENDING' },
        { step_order: 3, status: 'PENDING' },
      ],
    }
    mockDb.executionPollCount[executionId] = 0
    return HttpResponse.json(
      {
        execution_id: executionId,
        status: 'RUNNING',
        pipeline_id: pipe.id,
        started_at: nowIso(),
      },
      { status: 202 },
    )
  }),

  http.get('/api/v1/pipelines/:id/executions', ({ params, request }) => {
    const tid = tenantId(request)
    const pipelineId = String(params.id)
    const pipe = mockDb.pipelines.find(
      (p) => p.id === pipelineId && p.tenantId === tid,
    )
    if (!pipe) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    const rows = Object.values(mockDb.executions)
      .filter((e) => e.pipeline_id === pipelineId && e.tenant_id === tid)
      .map((e) => ({
        id: e.id,
        pipeline_id: e.pipeline_id,
        tenant_id: e.tenant_id,
        pipeline_version: e.pipeline_version ?? pipe.version,
        status: e.status,
        trigger: e.trigger ?? 'manual',
        started_at: e.started_at,
        completed_at: e.completed_at,
        records_in: e.records_in ?? 0,
        records_out: e.records_out ?? 0,
        completeness_pct: e.completeness_pct ?? null,
      }))
      .sort((a, b) => {
        const ta = a.started_at ? Date.parse(a.started_at) : 0
        const tb = b.started_at ? Date.parse(b.started_at) : 0
        return tb - ta
      })
    return HttpResponse.json(rows)
  }),

  http.get('/api/v1/pipelines/:id/executions/:executionId', ({ params }) => {
    const executionId = String(params.executionId)
    const current = mockDb.executions[executionId]
    if (!current) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    const count = (mockDb.executionPollCount[executionId] ?? 0) + 1
    mockDb.executionPollCount[executionId] = count
    if (count >= 2) {
      mockDb.executions[executionId] = {
        ...current,
        status: 'COMPLETED',
        completed_at: nowIso(),
        records_in: 10,
        records_out: 10,
        completeness_pct: 100,
        steps: [
          { step_order: 1, status: 'COMPLETED' },
          { step_order: 2, status: 'COMPLETED' },
          { step_order: 3, status: 'COMPLETED' },
        ],
      }
    } else if (count === 1) {
      mockDb.executions[executionId] = {
        ...current,
        status: 'RUNNING',
        steps: [
          { step_order: 1, status: 'COMPLETED' },
          { step_order: 2, status: 'RUNNING' },
          { step_order: 3, status: 'PENDING' },
        ],
      }
    }
    return HttpResponse.json(mockDb.executions[executionId])
  }),

  http.post(
    '/api/v1/pipelines/:id/executions/:executionId/cancel',
    ({ params, request }) => {
      const tid = tenantId(request)
      const pipelineId = String(params.id)
      const executionId = String(params.executionId)
      const pipe = mockDb.pipelines.find(
        (p) => p.id === pipelineId && p.tenantId === tid,
      )
      if (!pipe) {
        return HttpResponse.json({ message: 'Not found' }, { status: 404 })
      }
      const current = mockDb.executions[executionId]
      if (
        !current ||
        current.pipeline_id !== pipelineId ||
        current.tenant_id !== tid
      ) {
        return HttpResponse.json({ message: 'Not found' }, { status: 404 })
      }
      const terminal = ['COMPLETED', 'FAILED', 'CANCELLED'].includes(
        String(current.status).toUpperCase(),
      )
      if (!terminal) {
        mockDb.executions[executionId] = {
          ...current,
          status: 'CANCELLED',
          completed_at: nowIso(),
          error_summary: '{"message":"Force stopped by user"}',
          steps: (current.steps ?? []).map((s) => ({
            ...s,
            status:
              String(s.status).toUpperCase() === 'COMPLETED'
                ? 'COMPLETED'
                : 'CANCELLED',
          })),
        }
      }
      return HttpResponse.json(mockDb.executions[executionId])
    },
  ),
]

export const observabilityHandlers = [
  http.get('/api/v1/observability/links', ({ request }) => {
    const url = new URL(request.url)
    const pipelineId = url.searchParams.get('pipelineId')
    const executionId = url.searchParams.get('executionId')
    const qs = new URLSearchParams()
    if (pipelineId) qs.set('pipelineId', pipelineId)
    if (executionId) qs.set('executionId', executionId)
    const suffix = qs.toString() ? `?${qs}` : ''
    return HttpResponse.json({
      grafana_enabled: true,
      grafana_url: `http://localhost:3000${suffix}`,
      grafana_label: 'Grafana',
      elasticsearch_enabled: true,
      elasticsearch_url: `http://localhost:5601${suffix}`,
      elasticsearch_label: 'Elasticsearch',
    })
  }),

  http.get('/api/v1/observability/pipelines/:id/errors', ({ params, request }) => {
    const tid = tenantId(request)
    return HttpResponse.json({
      pipeline_id: params.id,
      tenant_id: tid,
      total_errors: 0,
      by_type: [],
    })
  }),

  http.get('/api/v1/observability/pipelines/:id/completeness', ({ params, request }) => {
    const tid = tenantId(request)
    return HttpResponse.json({
      pipeline_id: params.id,
      tenant_id: tid,
      execution_id: 'exec-fixture',
      records_in: 1000,
      records_out: 980,
      completeness_pct: 98,
      completeness_ratio: 0.98,
    })
  }),

  http.get('/api/v1/observability/pipelines/:id/latency', ({ params, request }) => {
    const tid = tenantId(request)
    return HttpResponse.json({
      pipeline_id: params.id,
      tenant_id: tid,
      sample_count: 42,
      mean_ms: 48.5,
      max_ms: 210,
      p50_ms: 40,
      p95_ms: 120,
      p99_ms: 180,
    })
  }),

  http.get('/api/v1/observability/pipelines/:id/heartbeat', ({ params, request }) => {
    const tid = tenantId(request)
    return HttpResponse.json({
      pipeline_id: params.id,
      tenant_id: tid,
      last_heartbeat_epoch_seconds: Math.floor(Date.now() / 1000) - 30,
      stale: false,
    })
  }),

  http.get('/api/v1/observability/executions/:execId/logs', ({ params, request }) => {
    const tid = tenantId(request)
    const execId = String(params.execId)
    const exec = mockDb.executions[execId]
    return HttpResponse.json({
      execution_id: execId,
      tenant_id: tid,
      pipeline_id: exec?.pipeline_id ?? 'pipe-unknown',
      logs: [
        {
          '@timestamp': nowIso(),
          level: 'INFO',
          pipelet_id: 'plet-s3-source',
          pod_name: `exec-${execId.slice(0, 8)}-stage-1`,
          message: 'stage started (MSW fixture)',
          records_in: 0,
          records_out: 12,
          duration_ms: 120,
        },
        {
          '@timestamp': nowIso(),
          level: 'INFO',
          pipelet_id: 'plet-webhook-destination',
          pod_name: `exec-${execId.slice(0, 8)}-stage-5`,
          message: 'uploaded inventory batch',
          records_in: 12,
          records_out: 12,
          duration_ms: 80,
        },
      ],
    })
  }),
]

function requireMatchingTenant(request: Request, pathId: string) {
  const tid = tenantId(request)
  if (tid !== pathId) {
    return HttpResponse.json({ message: 'Not found' }, { status: 404 })
  }
  return null
}

export const billingHandlers = [
  http.get('/api/v1/tenants/:id/usage', ({ params, request }) => {
    const denied = requireMatchingTenant(request, String(params.id))
    if (denied) {
      return denied
    }
    const now = new Date()
    const start = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1))
    const end = new Date(
      Date.UTC(now.getUTCFullYear(), now.getUTCMonth() + 1, 0, 23, 59, 59),
    )
    return HttpResponse.json({
      tenant_id: params.id,
      period_start: start.toISOString(),
      period_end: end.toISOString(),
      dimensions: {
        'platform.pipeline_runs': { quantity: 12, cost: 0.12 },
        'data.records_processed': { quantity: 125000, cost: 1.25 },
        'compute.vcpu_seconds': { quantity: 840, cost: 4.2 },
      },
      total_cost: 5.57,
      credit_balance: 94.43,
    })
  }),

  http.get('/api/v1/tenants/:id/usage/events', ({ params, request }) => {
    const denied = requireMatchingTenant(request, String(params.id))
    if (denied) {
      return denied
    }
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '20')
    const items = Array.from({ length: Math.min(size, 5) }, (_, i) => ({
      id: `ue-${page}-${i}`,
      dimension: i % 2 === 0 ? 'platform.pipeline_runs' : 'data.records_processed',
      quantity: i % 2 === 0 ? 1 : 100,
      unit: i % 2 === 0 ? 'runs' : 'records',
      execution_id: `exec-${i}`,
      pipeline_id: 'pipe-demo',
      pipelet_id: null,
      connector_id: null,
      recorded_at: nowIso(),
      idempotency_key: `idem-${page}-${i}`,
    }))
    return HttpResponse.json({
      tenant_id: params.id,
      items,
      page,
      size,
      total_elements: 12,
      total_pages: 3,
    })
  }),

  http.get('/api/v1/tenants/:id/quota', ({ params, request }) => {
    const denied = requireMatchingTenant(request, String(params.id))
    if (denied) {
      return denied
    }
    return HttpResponse.json({
      tenant_id: params.id,
      decision: 'ALLOW',
      message: 'Within soft and hard limits',
      allowed: true,
      credit_balance: 94.43,
      breached_dimension: null,
      soft_limit: null,
      hard_limit: null,
      current_usage: null,
      dimensions: {
        'platform.pipeline_runs': { soft: 1000, hard: 5000, usage: 12 },
        'data.records_processed': { soft: 1_000_000, hard: 5_000_000, usage: 125000 },
      },
    })
  }),

  http.get('/api/v1/tenants/:id/billing/periods', ({ params, request }) => {
    const denied = requireMatchingTenant(request, String(params.id))
    if (denied) {
      return denied
    }
    const now = new Date()
    const y = now.getUTCFullYear()
    const m = String(now.getUTCMonth() + 1).padStart(2, '0')
    const lastDay = new Date(Date.UTC(y, now.getUTCMonth() + 1, 0)).getUTCDate()
    return HttpResponse.json({
      tenant_id: params.id,
      periods: [
        {
          id: `bp-${y}-${m}`,
          period_start: `${y}-${m}-01`,
          period_end: `${y}-${m}-${String(lastDay).padStart(2, '0')}`,
          total_cost: 5.57,
          status: 'open',
        },
      ],
    })
  }),
]

export const tenantHandlers = [
  http.get('/api/v1/tenants', () => HttpResponse.json(mockDb.tenants)),

  http.get('/api/v1/tenants/_context', ({ request }) =>
    HttpResponse.json({ tenantId: tenantId(request) }),
  ),

  http.get('/api/v1/tenants/:id', ({ params }) => {
    const id = String(params.id)
    // Let billing/usage routes fall through if somehow matched — they are
    // registered separately with longer paths.
    const row = mockDb.tenants.find((t) => t.id === id)
    if (!row) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    return HttpResponse.json(row)
  }),

  http.post('/api/v1/tenants', async ({ request }) => {
    const body = (await request.json()) as CreateTenantRequest
    if (!body.name?.trim() || !body.slug?.trim()) {
      return HttpResponse.json({ message: 'Validation failed' }, { status: 400 })
    }
    const slug = body.slug.trim().toLowerCase()
    if (mockDb.tenants.some((t) => t.slug === slug)) {
      return HttpResponse.json(
        { message: `slug already exists: ${slug}` },
        { status: 409 },
      )
    }
    const created: Tenant = {
      id: id('tenant'),
      name: body.name.trim(),
      slug,
      status: body.status ?? 'trial',
      creditBalance: 100,
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }
    mockDb.tenants.push(created)
    return HttpResponse.json(created, { status: 201 })
  }),

  http.put('/api/v1/tenants/:id', async ({ params, request }) => {
    const idx = mockDb.tenants.findIndex((t) => t.id === params.id)
    if (idx < 0) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 })
    }
    const body = (await request.json()) as UpdateTenantRequest
    const prev = mockDb.tenants[idx]
    const updated: Tenant = {
      ...prev,
      name: body.name?.trim() || prev.name,
      status: body.status ?? prev.status,
      updatedAt: nowIso(),
    }
    mockDb.tenants[idx] = updated
    return HttpResponse.json(updated)
  }),
]

export const handlers = [
  ...billingHandlers,
  ...tenantHandlers,
  ...connectorHandlers,
  ...serviceHandlers,
  ...pipeletHandlers,
  ...pipelineHandlers,
  ...observabilityHandlers,
]
