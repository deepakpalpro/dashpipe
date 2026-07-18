package com.dashpipe.api.webhook;

/** Watched webhook input queue (tenant + connector scoped). */
public record WebhookQueueWatchTarget(String tenantId, String connectorId, String queueName) {}
