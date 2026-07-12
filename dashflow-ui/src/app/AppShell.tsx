import { NavLink } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useTenant } from '../contexts/TenantContext'
import { NAV_ITEMS } from './navItems'
import { AppRoutes } from './routes'

export function AppShell() {
  const { displayName, isAuthenticated } = useAuth()
  const { tenantId, tenantName, setTenantId, tenants } = useTenant()

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">
          <span className="brand-mark">Pipeline</span>
          <span className="brand-sub">Platform</span>
        </div>
        <nav className="primary-nav" aria-label="Primary">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                isActive ? 'nav-link active' : 'nav-link'
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="header-meta">
          <label className="tenant-picker">
            <span className="sr-only">Tenant</span>
            <select
              aria-label="Tenant"
              value={tenantId}
              onChange={(e) => setTenantId(e.target.value)}
            >
              {tenants.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.id})
                </option>
              ))}
            </select>
          </label>
          <span className="session-label" data-testid="session-label">
            {isAuthenticated ? displayName : 'Signed out'} · {tenantName}
          </span>
        </div>
      </header>
      <main className="app-main">
        <AppRoutes />
      </main>
    </div>
  )
}
