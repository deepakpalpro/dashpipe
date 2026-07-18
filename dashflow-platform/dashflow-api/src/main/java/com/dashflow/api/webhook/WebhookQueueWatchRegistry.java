package com.dashflow.api.webhook;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Tracks webhook `.in` queues that may need on-demand processor Jobs (W3-US06). */
@Component
public class WebhookQueueWatchRegistry {

  private final ConcurrentHashMap<String, WebhookQueueWatchTarget> targets = new ConcurrentHashMap<>();

  public void register(String tenantId, String connectorId, String queueName) {
    if (tenantId == null || connectorId == null || queueName == null) {
      return;
    }
    String key = key(tenantId, connectorId);
    targets.put(key, new WebhookQueueWatchTarget(tenantId, connectorId, queueName));
  }

  public Collection<WebhookQueueWatchTarget> snapshot() {
    return targets.values();
  }

  public void clear() {
    targets.clear();
  }

  static String key(String tenantId, String connectorId) {
    return tenantId + ":" + connectorId;
  }
}
