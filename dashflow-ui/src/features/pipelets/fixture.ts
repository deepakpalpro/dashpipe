import type { PipeletCatalogEntry } from './catalogFilter'
import fixture from '../../fixtures/pipelets.json'

/**
 * Catalog pipelets default to inactive until a runnable image/binary exists.
 * Explicit `active: true` in the JSON (or registration) opts a pipelet into the builder palette.
 */
export const PIPELET_FIXTURE: PipeletCatalogEntry[] = (
  fixture as PipeletCatalogEntry[]
).map((p) => ({
  ...p,
  active: p.active === true,
}))
