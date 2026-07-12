/** Build headers for future `/api/v1/*` calls from TenantContext. */
export function buildApiHeaders(tenantId: string): HeadersInit {
  return {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    'X-Tenant-Id': tenantId,
  }
}

export async function apiFetch(
  path: string,
  tenantId: string,
  init: RequestInit = {},
): Promise<Response> {
  const headers = {
    ...buildApiHeaders(tenantId),
    ...(init.headers ?? {}),
  }
  return fetch(path, { ...init, headers })
}
