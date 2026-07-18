/** Shallow merge: overrides win and may add keys (extend). */
export function mergeExtendConfig(
  defaults: Record<string, unknown> | null | undefined,
  overrides: Record<string, unknown> | null | undefined,
): Record<string, unknown> {
  return { ...(defaults ?? {}), ...(overrides ?? {}) }
}
