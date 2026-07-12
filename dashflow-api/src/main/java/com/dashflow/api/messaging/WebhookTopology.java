package com.dashflow.api.messaging;

/**
 * Declared webhook ingress topology for one tenant connector (architecture §11.5).
 *
 * @param exchange topic exchange {@code tenant.{T}.webhook}
 * @param inputQueue {@code ….{connectorId}.in}
 * @param dlq {@code ….{connectorId}.dlq}
 * @param routingKey {@code {connectorId}}
 */
public record WebhookTopology(
    String tenantId, String connectorId, String exchange, String inputQueue, String dlq, String routingKey) {}
