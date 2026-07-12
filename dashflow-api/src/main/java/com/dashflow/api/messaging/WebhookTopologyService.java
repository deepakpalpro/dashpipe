package com.dashflow.api.messaging;

import com.dashflow.spi.messaging.LogicalDestinations;
import com.dashflow.spi.messaging.MessageBroker;
import org.springframework.stereotype.Service;

/** Declares tenant webhook destinations (architecture §11.5) via {@link MessageBroker}. */
@Service
public class WebhookTopologyService {

  private final MessageBroker messageBroker;

  public WebhookTopologyService(MessageBroker messageBroker) {
    this.messageBroker = messageBroker;
  }

  public WebhookTopology declare(String tenantId, String connectorId) {
    messageBroker.declareWebhookTopology(tenantId, connectorId);
    String exchangeName = LogicalDestinations.webhookExchange(tenantId);
    String inputQueue = LogicalDestinations.webhookInputQueue(tenantId, connectorId);
    String dlqName = LogicalDestinations.webhookDlq(tenantId, connectorId);
    String routingKey = LogicalDestinations.webhookRoutingKey(connectorId);
    return new WebhookTopology(
        tenantId, connectorId, exchangeName, inputQueue, dlqName, routingKey);
  }
}
