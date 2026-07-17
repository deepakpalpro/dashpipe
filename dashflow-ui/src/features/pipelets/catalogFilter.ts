export type PipeletCategory = 'Source' | 'Processor' | 'Destination'

export type PipeletRuntime = 'Java' | 'Python' | 'Binary'

export type PipeletStatusFilter = 'All' | 'Active' | 'Inactive'

export type PipeletScope = 'system' | 'tenant'

export type PipeletCatalogEntry = {
  id: string
  name: string
  category: PipeletCategory
  version: string
  runtime: PipeletRuntime | string
  description: string
  /**
   * Whether a runnable image/binary is available.
   * Catalog entries without a runtime image are inactive and hidden from the builder palette.
   */
  active?: boolean
  /** Docker image or binary path when a runtime exists. */
  imageRef?: string
  /**
   * Ownership: platform-shipped (`system`) vs tenant-registered (`tenant`).
   * Defaults via {@link enrichPipeletTaxonomy}.
   */
  scope?: PipeletScope
  /**
   * Domain group under the category (e.g. `http`, `database`).
   * Matches pipelets/&lt;root&gt;/&lt;group&gt;/ on disk for system pipelets.
   */
  group?: string
  /** Deployment KeyValue keys that must be set on the pipeline step (after defaults). */
  requiredDeploymentKeys?: string[]
  /** Execution KeyValue keys that must be set on the pipeline step (after defaults). */
  requiredExecutionKeys?: string[]
  configSchemaPreview?: Record<string, unknown>
  /** Defaults for where/how the pipelet is deployed; pipeline/step may override/extend. */
  deploymentConfiguration?: Record<string, unknown>
  /** Defaults for runtime behavior; pipeline/step may override/extend. */
  executionConfiguration?: Record<string, unknown>
}

export type CatalogFilterInput = {
  category?: PipeletCategory | 'All'
  search?: string
  /** Listing filter. Palette should pass `Active` (or use {@link activePipelets}). */
  status?: PipeletStatusFilter
  scope?: PipeletScope | 'All'
  group?: string | 'All'
}

export function isPipeletActive(item: PipeletCatalogEntry): boolean {
  return item.active === true
}

export function activePipelets(
  items: PipeletCatalogEntry[],
): PipeletCatalogEntry[] {
  return items.filter(isPipeletActive)
}

export function catalogFilter(
  items: PipeletCatalogEntry[],
  input: CatalogFilterInput = {},
): PipeletCatalogEntry[] {
  const category = input.category ?? 'All'
  const search = (input.search ?? '').trim().toLowerCase()
  const status = input.status ?? 'All'
  const scope = input.scope ?? 'All'
  const group = input.group ?? 'All'

  return items.filter((item) => {
    if (category !== 'All' && item.category !== category) {
      return false
    }
    if (scope !== 'All' && (item.scope ?? 'system') !== scope) {
      return false
    }
    if (group !== 'All' && (item.group ?? 'custom') !== group) {
      return false
    }
    if (status === 'Active' && !isPipeletActive(item)) {
      return false
    }
    if (status === 'Inactive' && isPipeletActive(item)) {
      return false
    }
    if (!search) {
      return true
    }
    return (
      item.name.toLowerCase().includes(search) ||
      item.id.toLowerCase().includes(search) ||
      item.description.toLowerCase().includes(search) ||
      (item.group ?? '').toLowerCase().includes(search)
    )
  })
}
