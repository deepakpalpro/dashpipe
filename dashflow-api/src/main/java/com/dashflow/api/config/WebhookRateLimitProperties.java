package com.dashflow.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Per-tenant webhook ingress rate limit (W3-US04). */
@ConfigurationProperties(prefix = "pipeline.webhook.rate-limit")
public class WebhookRateLimitProperties {

  private boolean enabled = true;
  private int requestsPerWindow = 120;
  private int windowSeconds = 60;
  private int retryAfterSeconds = 60;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getRequestsPerWindow() {
    return requestsPerWindow;
  }

  public void setRequestsPerWindow(int requestsPerWindow) {
    this.requestsPerWindow = requestsPerWindow;
  }

  public int getWindowSeconds() {
    return windowSeconds;
  }

  public void setWindowSeconds(int windowSeconds) {
    this.windowSeconds = windowSeconds;
  }

  public int getRetryAfterSeconds() {
    return retryAfterSeconds;
  }

  public void setRetryAfterSeconds(int retryAfterSeconds) {
    this.retryAfterSeconds = retryAfterSeconds;
  }
}
