import type { PipeletCatalogEntry } from './catalogFilter'
import fixture from '../../fixtures/pipelets.json'
import { getActivatedPipeletIds } from './pipeletActivation'
import { enrichCatalog } from './pipeletTaxonomy'

/**
 * Catalog pipelets default to inactive until a runnable image/binary exists.
 * Explicit `active: true` in the JSON (or registration) opts a pipelet into the builder palette.
 * Session activations (e.g. pipeline import) are merged via {@link getPipeletCatalog}.
 */
export const PIPELET_FIXTURE: PipeletCatalogEntry[] = enrichCatalog(
  (fixture as PipeletCatalogEntry[]).map((p) => ({
    ...p,
    active: p.active === true,
  })),
)

/** Fixture merged with session-activated pipelet IDs (import / register). */
export function getPipeletCatalog(
  activatedIds: Iterable<string> = getActivatedPipeletIds(),
): PipeletCatalogEntry[] {
  const activated = new Set(
    [...activatedIds].map((id) => id.trim()).filter(Boolean),
  )
  if (activated.size === 0) {
    return PIPELET_FIXTURE.map((p) => ({ ...p }))
  }
  return PIPELET_FIXTURE.map((p) =>
    activated.has(p.id) ? { ...p, active: true } : { ...p },
  )
}
