const STORAGE_KEY = 'dashpipe.pipelet.activatedIds'

function readStore(): Set<string> {
  if (typeof window === 'undefined') {
    return new Set()
  }
  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return new Set()
    }
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) {
      return new Set()
    }
    return new Set(
      parsed.filter((id): id is string => typeof id === 'string' && id.length > 0),
    )
  } catch {
    return new Set()
  }
}

function writeStore(ids: Set<string>) {
  if (typeof window === 'undefined') {
    return
  }
  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify([...ids].sort()))
}

/** Pipelet IDs activated in this browser session (e.g. via pipeline import). */
export function getActivatedPipeletIds(): Set<string> {
  return readStore()
}

/**
 * Mark pipelets as active for the catalog / builder palette.
 * Returns the IDs that were newly activated (already-active IDs omitted).
 */
export function activatePipelets(ids: Iterable<string>): string[] {
  const next = readStore()
  const newly: string[] = []
  for (const id of ids) {
    const trimmed = id?.trim()
    if (!trimmed) {
      continue
    }
    if (!next.has(trimmed)) {
      newly.push(trimmed)
      next.add(trimmed)
    }
  }
  if (newly.length > 0) {
    writeStore(next)
  }
  return newly
}

/** Extract distinct pipelet_id values from a pipeline bundle's steps. */
export function pipeletIdsFromBundleSteps(
  steps: Array<{ pipelet_id?: string | null }> | null | undefined,
): string[] {
  if (!steps?.length) {
    return []
  }
  const ids = new Set<string>()
  for (const step of steps) {
    const id = step.pipelet_id?.trim()
    if (id) {
      ids.add(id)
    }
  }
  return [...ids]
}

/** Test helper — clears session activations. */
export function clearActivatedPipelets(): void {
  if (typeof window === 'undefined') {
    return
  }
  window.sessionStorage.removeItem(STORAGE_KEY)
}
