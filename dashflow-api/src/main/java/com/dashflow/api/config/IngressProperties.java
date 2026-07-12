package com.dashflow.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Public webhook ingress base URL (W3-US05). */
@ConfigurationProperties(prefix = "pipeline.ingress")
public class IngressProperties {

  /** Scheme + host (+ optional port), no trailing slash. */
  private String baseUrl = "http://localhost:8080";

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** Trim trailing slashes for stable URL joining. */
  public String normalizedBaseUrl() {
    if (baseUrl == null || baseUrl.isBlank()) {
      return "http://localhost:8080";
    }
    String trimmed = baseUrl.trim();
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }
}
