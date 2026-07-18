package com.dashpipe.api.usage;

import java.time.Instant;

/**
 * Usage / billing dimension event (architecture §6.2 / §11.7). Wave 3 emits from webhook ingress;
 * Wave 5 collector persists / aggregates.
 */
public record UsageEvent(
    String dimension,
    double amount,
    String tenantId,
    String connectorId,
    Instant occurredAt,
    String executionId,
    String pipelineId,
    String pipeletId,
    String unit,
    String idempotencyKey) {

  public static final String WEBHOOK_EVENTS = "platform.webhook_events";
  public static final String BYTES_IN = "data.bytes_in";

  /** Architecture §6.2 / Wave 5 pipelet stub dimensions. */
  public static final String RECORDS_PROCESSED = "data.records_processed";
  public static final String VCPU_SECONDS = "compute.vcpu_seconds";
  public static final String CONNECTOR_API_CALLS = "connector.api_calls";
  public static final String PIPELINE_RUNS = "platform.pipeline_runs";

  /** W3 webhook / simple emit shape. */
  public UsageEvent(
      String dimension, double amount, String tenantId, String connectorId, Instant occurredAt) {
    this(dimension, amount, tenantId, connectorId, occurredAt, null, null, null, null, null);
  }

  public UsageEvent withIdempotencyKey(String key) {
    return new UsageEvent(
        dimension,
        amount,
        tenantId,
        connectorId,
        occurredAt,
        executionId,
        pipelineId,
        pipeletId,
        unit,
        key);
  }
}
