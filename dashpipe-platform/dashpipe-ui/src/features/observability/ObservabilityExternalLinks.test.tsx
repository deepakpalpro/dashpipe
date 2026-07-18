import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ObservabilityExternalLinks } from './ObservabilityExternalLinks'

describe('ObservabilityExternalLinks', () => {
  it('renders nothing when tools are disabled', () => {
    const { container } = render(
      <ObservabilityExternalLinks
        links={{
          grafana_enabled: false,
          grafana_url: null,
          grafana_label: 'Grafana',
          elasticsearch_enabled: false,
          elasticsearch_url: null,
          elasticsearch_label: 'Elasticsearch',
        }}
      />,
    )
    expect(container).toBeEmptyDOMElement()
  })

  it('renders only enabled tool links', () => {
    render(
      <ObservabilityExternalLinks
        links={{
          grafana_enabled: true,
          grafana_url: 'http://localhost:3000',
          grafana_label: 'Grafana',
          elasticsearch_enabled: true,
          elasticsearch_url: 'http://localhost:5601',
          elasticsearch_label: 'Logs',
        }}
      />,
    )
    expect(screen.getByRole('link', { name: /Grafana/i })).toHaveAttribute(
      'href',
      'http://localhost:3000',
    )
    expect(screen.getByRole('link', { name: /Logs/i })).toHaveAttribute(
      'href',
      'http://localhost:5601',
    )
  })
})
