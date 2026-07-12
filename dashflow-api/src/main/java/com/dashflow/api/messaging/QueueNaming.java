package com.dashflow.api.messaging;

import com.dashflow.spi.messaging.LogicalDestinations;

/**
 * Tenant-prefixed logical destination names (broker-agnostic). Delegates to {@link
 * LogicalDestinations} in dashflow-spi.
 */
public final class QueueNaming {

  private QueueNaming() {}

  public static String pipelineExchange(String tenantId, String pipelineId) {
    return LogicalDestinations.pipelineExchange(tenantId, pipelineId);
  }

  public static String stageInputQueue(String tenantId, String pipelineId, int stageOrder) {
    return LogicalDestinations.stageInputQueue(tenantId, pipelineId, stageOrder);
  }

  public static String stageDlq(String tenantId, String pipelineId, int stageOrder) {
    return LogicalDestinations.stageDlq(tenantId, pipelineId, stageOrder);
  }

  public static String stageOutputQueue(String tenantId, String pipelineId, int stageOrder) {
    return LogicalDestinations.stageOutputQueue(tenantId, pipelineId, stageOrder);
  }

  public static String stageRoutingKey(int stageOrder) {
    return LogicalDestinations.stageRoutingKey(stageOrder);
  }

  public static String deadLetterExchange(String tenantId, String pipelineId) {
    return LogicalDestinations.deadLetterExchange(tenantId, pipelineId);
  }

  public static String stageDlqRoutingKey(int stageOrder) {
    return LogicalDestinations.stageDlqRoutingKey(stageOrder);
  }

  public static String webhookExchange(String tenantId) {
    return LogicalDestinations.webhookExchange(tenantId);
  }

  public static String webhookInputQueue(String tenantId, String connectorId) {
    return LogicalDestinations.webhookInputQueue(tenantId, connectorId);
  }

  public static String webhookDlq(String tenantId, String connectorId) {
    return LogicalDestinations.webhookDlq(tenantId, connectorId);
  }

  public static String webhookRoutingKey(String connectorId) {
    return LogicalDestinations.webhookRoutingKey(connectorId);
  }
}
