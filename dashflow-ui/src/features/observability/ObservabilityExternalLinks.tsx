import type { ObservabilityPortalLinks } from '../../../api/types'

type Props = {
  links: ObservabilityPortalLinks | null | undefined
  loading?: boolean
}

export function ObservabilityExternalLinks({ links, loading }: Props) {
  if (loading || !links) {
    return null
  }
  const items: { href: string; label: string }[] = []
  if (links.grafana_enabled && links.grafana_url) {
    items.push({
      href: links.grafana_url,
      label: links.grafana_label || 'Grafana',
    })
  }
  if (links.elasticsearch_enabled && links.elasticsearch_url) {
    items.push({
      href: links.elasticsearch_url,
      label: links.elasticsearch_label || 'Elasticsearch',
    })
  }
  if (items.length === 0) {
    return null
  }

  return (
    <div className="obs-external-links" aria-label="External observability tools">
      {items.map((item) => (
        <a
          key={item.href}
          className="obs-external-link"
          href={item.href}
          target="_blank"
          rel="noopener noreferrer"
        >
          {item.label}
          <span className="sr-only"> (opens in new tab)</span>
        </a>
      ))}
    </div>
  )
}
