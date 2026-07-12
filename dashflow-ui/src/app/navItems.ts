export const NAV_ITEMS = [
  { label: 'Pipelets', path: '/pipelets' },
  { label: 'Pipelines', path: '/pipelines' },
  { label: 'Connectors', path: '/connectors' },
  { label: 'Services', path: '/services' },
  { label: 'Tenants', path: '/tenants' },
  { label: 'Observability', path: '/observability' },
  { label: 'Billing', path: '/billing' },
] as const

export type NavPath = (typeof NAV_ITEMS)[number]['path']
