package com.dashpipe.api.usage;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emits webhook metering dimensions (architecture §11.7). Failures are logged — never fail the
 * HTTP accept path.
 */
@Component
public class UsageEventEmitter {

  private static final Logger log = LoggerFactory.getLogger(UsageEventEmitter.class);

  private final UsageEventCollector collector;

  public UsageEventEmitter(UsageEventCollector collector) {
    this.collector = collector;
  }

  /** Record one accepted webhook delivery (logical event) and its payload size. */
  public void emitWebhookAccepted(String tenantId, String connectorId, long bytesIn) {
    Instant now = Instant.now();
    try {
      collector.collect(
          new UsageEvent(UsageEvent.WEBHOOK_EVENTS, 1.0, tenantId, connectorId, now)
              .withIdempotencyKey(webhookKey(tenantId, connectorId, UsageEvent.WEBHOOK_EVENTS, now, 1.0)));
      collector.collect(
          new UsageEvent(
                  UsageEvent.BYTES_IN, Math.max(0L, bytesIn), tenantId, connectorId, now)
              .withIdempotencyKey(
                  webhookKey(
                      tenantId, connectorId, UsageEvent.BYTES_IN, now, Math.max(0L, bytesIn))));
    } catch (Exception ex) {
      log.warn(
          "Usage metering failed tenant={} connector={} (accept still succeeds): {}",
          tenantId,
          connectorId,
          ex.toString());
    }
  }

  private static String webhookKey(
      String tenantId, String connectorId, String dimension, Instant when, double amount) {
    return "webhook:"
        + tenantId
        + ":"
        + connectorId
        + ":"
        + dimension
        + ":"
        + when.toEpochMilli()
        + ":"
        + amount;
  }
}
