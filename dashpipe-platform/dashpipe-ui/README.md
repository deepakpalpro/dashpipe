# dashpipe-ui

Vite + React + TypeScript frontend for the Dashpipe (Wave 6+).

## Scripts

```bash
npm install
npm run dev          # MSW on by default in DEV
npm run dev:api      # hit live API (VITE_ENABLE_MSW=false)
npm test
npm run build
```

## Layout

```text
src/
  app/        # AppShell, routes, nav items
  contexts/   # AuthContext, TenantContext
  api/        # apiClient, resources, secret helpers
  features/   # connectors, services, placeholders
  mocks/      # MSW handlers (W1-shaped fixtures)
  test/       # setup + renderWithProviders
```

KB: [`docs/delivery/kb/W6-US01-nav-shell.md`](../docs/delivery/kb/W6-US01-nav-shell.md), [`W6-US02-connectors-services-ui.md`](../docs/delivery/kb/W6-US02-connectors-services-ui.md).
