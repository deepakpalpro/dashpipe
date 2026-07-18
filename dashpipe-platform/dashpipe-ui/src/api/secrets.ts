/** Mirrors W1 SecretRedactor keys — never display raw values for these. */
const SECRET_KEYS = new Set([
  'client_secret',
  'signing_secret',
  'api_key',
  'password',
  'secret',
  'private_key',
  'access_token',
])

export const REDACTED = '***'
export const MASK_DISPLAY = '••••••'

export function isSecretKey(name: string): boolean {
  const key = name.toLowerCase()
  const compact = key.replace(/[-_]/g, '')
  return (
    SECRET_KEYS.has(key) ||
    key.endsWith('_secret') ||
    key.endsWith('_password') ||
    compact.includes('accesskey') ||
    compact.includes('secretkey') ||
    compact === 'apikey'
  )
}

export function redactConfig(
  config: Record<string, unknown> | null | undefined,
): Record<string, unknown> {
  if (!config || typeof config !== 'object') {
    return {}
  }
  const out: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(config)) {
    out[k] = isSecretKey(k) ? REDACTED : v
  }
  return out
}

/** Value shown in read-only UI for a config field. */
export function displayConfigValue(key: string, value: unknown): string {
  if (isSecretKey(key) || value === REDACTED) {
    return MASK_DISPLAY
  }
  if (value === null || value === undefined) {
    return ''
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}
