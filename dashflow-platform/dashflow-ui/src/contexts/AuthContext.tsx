import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { isAdminRole } from './roleGate'

export type AuthSession = {
  isAuthenticated: boolean
  displayName: string
}

type AuthContextValue = AuthSession & {
  isAdmin: boolean
  signInStub: (displayName?: string) => void
  signOutStub: () => void
}

const DEFAULT_SESSION: AuthSession = {
  isAuthenticated: true,
  displayName: 'Platform Operator',
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession>(DEFAULT_SESSION)

  const signInStub = useCallback((displayName = 'Platform Operator') => {
    setSession({ isAuthenticated: true, displayName })
  }, [])

  const signOutStub = useCallback(() => {
    setSession({ isAuthenticated: false, displayName: '' })
  }, [])

  const value = useMemo(
    () => ({
      ...session,
      isAdmin: isAdminRole(session.displayName, session.isAuthenticated),
      signInStub,
      signOutStub,
    }),
    [session, signInStub, signOutStub],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
