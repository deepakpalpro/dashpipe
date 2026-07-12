package com.dashflow.api.webhook;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.stereotype.Component;

/** Reads RabbitMQ queue depth via AMQP queue info (W3-US06). */
@Component
public class WebhookQueueDepthReader {

  private final AmqpAdmin amqpAdmin;

  public WebhookQueueDepthReader(AmqpAdmin amqpAdmin) {
    this.amqpAdmin = amqpAdmin;
  }

  public long messageCount(String queueName) {
    if (queueName == null || queueName.isBlank()) {
      return 0L;
    }
    QueueInformation info = amqpAdmin.getQueueInfo(queueName);
    if (info == null) {
      return 0L;
    }
    return info.getMessageCount();
  }
}
