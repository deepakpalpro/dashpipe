import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { PipeletCatalogEntry } from './catalogFilter'
import { getPipeletCatalog } from './fixture'
import {
  activatePipelets as persistActivatePipelets,
  getActivatedPipeletIds,
} from './pipeletActivation'

type PipeletCatalogContextValue = {
  catalog: PipeletCatalogEntry[]
  /** Activate catalog entries by id (palette + Active filter). Persists for the session. */
  activatePipelets: (ids: Iterable<string>) => string[]
  refreshCatalog: () => void
}

const PipeletCatalogContext = createContext<PipeletCatalogContextValue | null>(
  null,
)

export function PipeletCatalogProvider({ children }: { children: ReactNode }) {
  const [catalog, setCatalog] = useState(() => getPipeletCatalog())

  const refreshCatalog = useCallback(() => {
    setCatalog(getPipeletCatalog())
  }, [])

  const activate = useCallback((ids: Iterable<string>) => {
    const newly = persistActivatePipelets(ids)
    // Always recompute so callers see active flags even if ids were already stored.
    setCatalog(getPipeletCatalog())
    return newly
  }, [])

  const value = useMemo(
    () => ({
      catalog,
      activatePipelets: activate,
      refreshCatalog,
    }),
    [catalog, activate, refreshCatalog],
  )

  return (
    <PipeletCatalogContext.Provider value={value}>
      {children}
    </PipeletCatalogContext.Provider>
  )
}

export function usePipeletCatalog(): PipeletCatalogContextValue {
  const ctx = useContext(PipeletCatalogContext)
  if (!ctx) {
    // Fallback for tests that render pages without the provider.
    const activated = getActivatedPipeletIds()
    return {
      catalog: getPipeletCatalog(activated),
      activatePipelets: (ids) => {
        const newly = persistActivatePipelets(ids)
        return newly
      },
      refreshCatalog: () => undefined,
    }
  }
  return ctx
}
