package com.dashflow.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional external observability tools for the portal. Leave URLs blank to hide links (customers
 * without Grafana / Elasticsearch).
 */
@ConfigurationProperties(prefix = "pipeline.observability")
public class ObservabilityPortalProperties {

  /** Grafana base or dashboard URL. Empty = link hidden. */
  private String grafanaBaseUrl = "";

  /** Elasticsearch or Kibana URL. Empty = link hidden. */
  private String elasticsearchBaseUrl = "";

  /** Optional button label override (default: Grafana). */
  private String grafanaLabel = "Grafana";

  /** Optional button label override (default: Elasticsearch). */
  private String elasticsearchLabel = "Elasticsearch";

  public String getGrafanaBaseUrl() {
    return grafanaBaseUrl;
  }

  public void setGrafanaBaseUrl(String grafanaBaseUrl) {
    this.grafanaBaseUrl = grafanaBaseUrl;
  }

  public String getElasticsearchBaseUrl() {
    return elasticsearchBaseUrl;
  }

  public void setElasticsearchBaseUrl(String elasticsearchBaseUrl) {
    this.elasticsearchBaseUrl = elasticsearchBaseUrl;
  }

  public String getGrafanaLabel() {
    return grafanaLabel;
  }

  public void setGrafanaLabel(String grafanaLabel) {
    this.grafanaLabel = grafanaLabel;
  }

  public String getElasticsearchLabel() {
    return elasticsearchLabel;
  }

  public void setElasticsearchLabel(String elasticsearchLabel) {
    this.elasticsearchLabel = elasticsearchLabel;
  }

  public String normalizedGrafanaUrl() {
    return normalize(grafanaBaseUrl);
  }

  public String normalizedElasticsearchUrl() {
    return normalize(elasticsearchBaseUrl);
  }

  public boolean isGrafanaEnabled() {
    return normalizedGrafanaUrl() != null;
  }

  public boolean isElasticsearchEnabled() {
    return normalizedElasticsearchUrl() != null;
  }

  private static String normalize(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.trim();
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed.isBlank() ? null : trimmed;
  }
}
