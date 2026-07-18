package com.dashpipe.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** On-demand webhook processor trigger (W3-US06). Disabled by default. */
@ConfigurationProperties(prefix = "pipeline.webhook.queue-trigger")
public class WebhookQueueTriggerProperties {

  /** When true, poll registered webhook `.in` queues and spawn Jobs on depth &gt; 0. */
  private boolean enabled = false;

  /** Poll interval in milliseconds. */
  private long pollIntervalMs = 500;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getPollIntervalMs() {
    return pollIntervalMs;
  }

  public void setPollIntervalMs(long pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
  }
}
