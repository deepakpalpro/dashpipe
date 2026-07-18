package com.dashflow.spi.messaging;

/**
 * Broker-agnostic logical destination names. Adapters map these to product primitives.
 *
 * <pre>
 * Exchange (where applicable): tenant.{tenantId}.pipeline.{pipelineId}
 * Queue:    tenant.{tenantId}.pipeline.{pipelineId}.stage.{n}.in
 * DLQ:      tenant.{tenantId}.pipeline.{pipelineId}.stage.{n}.dlq
 * </pre>
 */
public final class LogicalDestinations {

  private LogicalDestinations() {}

  public static String pipelineExchange(String tenantId, String pipelineId) {
    requireToken(tenantId, "tenantId");
    requireToken(pipelineId, "pipelineId");
    return "tenant." + tenantId + ".pipeline." + pipelineId;
  }

  public static String stageInputQueue(String tenantId, String pipelineId, int stageOrder) {
    requirePositiveStage(stageOrder);
    return pipelineExchange(tenantId, pipelineId) + ".stage." + stageOrder + ".in";
  }

  public static String stageDlq(String tenantId, String pipelineId, int stageOrder) {
    requirePositiveStage(stageOrder);
    return pipelineExchange(tenantId, pipelineId) + ".stage." + stageOrder + ".dlq";
  }

  /** Output of stage N is the input queue of stage N+1 (last stage has no platform output). */
  public static String stageOutputQueue(String tenantId, String pipelineId, int stageOrder) {
    requirePositiveStage(stageOrder);
    return stageInputQueue(tenantId, pipelineId, stageOrder + 1);
  }

  public static String stageRoutingKey(int stageOrder) {
    requirePositiveStage(stageOrder);
    return "stage." + stageOrder;
  }

  public static String deadLetterExchange(String tenantId, String pipelineId) {
    return pipelineExchange(tenantId, pipelineId) + ".dlx";
  }

  public static String stageDlqRoutingKey(int stageOrder) {
    requirePositiveStage(stageOrder);
    return "stage." + stageOrder + ".dlq";
  }

  public static String webhookExchange(String tenantId) {
    requireToken(tenantId, "tenantId");
    return "tenant." + tenantId + ".webhook";
  }

  public static String webhookInputQueue(String tenantId, String connectorId) {
    requireToken(tenantId, "tenantId");
    requireToken(connectorId, "connectorId");
    return webhookExchange(tenantId) + "." + connectorId + ".in";
  }

  public static String webhookDlq(String tenantId, String connectorId) {
    requireToken(tenantId, "tenantId");
    requireToken(connectorId, "connectorId");
    return webhookExchange(tenantId) + "." + connectorId + ".dlq";
  }

  public static String webhookRoutingKey(String connectorId) {
    requireToken(connectorId, "connectorId");
    return connectorId;
  }

  private static void requireToken(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
    if (value.contains(".") || value.contains(" ") || value.contains("/")) {
      throw new IllegalArgumentException(name + " must not contain '.', ' ', or '/'");
    }
  }

  private static void requirePositiveStage(int stageOrder) {
    if (stageOrder < 1) {
      throw new IllegalArgumentException("stageOrder must be >= 1");
    }
  }
}
