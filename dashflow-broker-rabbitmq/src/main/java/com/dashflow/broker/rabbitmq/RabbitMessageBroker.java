package com.dashflow.broker.rabbitmq;

import com.dashflow.spi.messaging.LogicalDestinations;
import com.dashflow.spi.messaging.MessageBroker;
import com.dashflow.spi.messaging.PipelineTopologyView;
import com.dashflow.spi.messaging.PipelineTopologyView.StageTopologyView;
import com.dashflow.spi.platform.PlatformAssembly;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(
    prefix = "dashflow.platform",
    name = "broker",
    havingValue = PlatformAssembly.BROKER_RABBITMQ,
    matchIfMissing = true)
public class RabbitMessageBroker implements MessageBroker {

  private final AmqpAdmin amqpAdmin;
  private final RabbitTemplate rabbitTemplate;
  private final RabbitProperties rabbitProperties;
  private final String amqpUrlOverride;

  public RabbitMessageBroker(
      AmqpAdmin amqpAdmin,
      RabbitTemplate rabbitTemplate,
      RabbitProperties rabbitProperties,
      org.springframework.core.env.Environment env) {
    this.amqpAdmin = amqpAdmin;
    this.rabbitTemplate = rabbitTemplate;
    this.rabbitProperties = rabbitProperties;
    this.amqpUrlOverride =
        firstNonBlank(
            env.getProperty("pipeline.orchestration.amqp-url"),
            env.getProperty("dashflow.orchestration.amqp-url"));
  }

  private static String firstNonBlank(String a, String b) {
    if (StringUtils.hasText(a)) {
      return a;
    }
    return b == null ? "" : b;
  }

  @Override
  public String id() {
    return PlatformAssembly.BROKER_RABBITMQ;
  }

  @Override
  public PipelineTopologyView declarePipelineTopology(
      String tenantId, String pipelineId, int stageCount) {
    if (stageCount < 1) {
      throw new IllegalArgumentException("stageCount must be >= 1");
    }

    String exchangeName = LogicalDestinations.pipelineExchange(tenantId, pipelineId);
    amqpAdmin.declareExchange(new TopicExchange(exchangeName, true, false));

    String dlxName = LogicalDestinations.deadLetterExchange(tenantId, pipelineId);
    TopicExchange deadLetterExchange = new TopicExchange(dlxName, true, false);
    amqpAdmin.declareExchange(deadLetterExchange);

    List<StageTopologyView> stages = new ArrayList<>();
    for (int stage = 1; stage <= stageCount; stage++) {
      String inputQueue = LogicalDestinations.stageInputQueue(tenantId, pipelineId, stage);
      String dlqName = LogicalDestinations.stageDlq(tenantId, pipelineId, stage);
      String routingKey = LogicalDestinations.stageRoutingKey(stage);
      String dlqRoutingKey = LogicalDestinations.stageDlqRoutingKey(stage);
      String outputQueue =
          stage < stageCount
              ? LogicalDestinations.stageOutputQueue(tenantId, pipelineId, stage)
              : null;

      Queue dlq = QueueBuilder.durable(dlqName).build();
      amqpAdmin.declareQueue(dlq);
      amqpAdmin.declareBinding(
          BindingBuilder.bind(dlq).to(deadLetterExchange).with(dlqRoutingKey));

      Queue queue =
          QueueBuilder.durable(inputQueue)
              .deadLetterExchange(dlxName)
              .deadLetterRoutingKey(dlqRoutingKey)
              .build();
      amqpAdmin.declareQueue(queue);
      amqpAdmin.declareBinding(
          BindingBuilder.bind(queue).to(new TopicExchange(exchangeName, true, false)).with(routingKey));

      stages.add(new StageTopologyView(stage, inputQueue, outputQueue, dlqName, routingKey));
    }

    return new PipelineTopologyView(tenantId, pipelineId, exchangeName, List.copyOf(stages));
  }

  @Override
  public void declareWebhookTopology(String tenantId, String connectorId) {
    String exchangeName = LogicalDestinations.webhookExchange(tenantId);
    TopicExchange exchange = new TopicExchange(exchangeName, true, false);
    amqpAdmin.declareExchange(exchange);

    String input = LogicalDestinations.webhookInputQueue(tenantId, connectorId);
    String dlq = LogicalDestinations.webhookDlq(tenantId, connectorId);
    String rk = LogicalDestinations.webhookRoutingKey(connectorId);

    amqpAdmin.declareQueue(QueueBuilder.durable(dlq).build());
    Queue queue = QueueBuilder.durable(input).build();
    amqpAdmin.declareQueue(queue);
    amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(rk));
  }

  @Override
  public void purgeDestinations(Collection<String> destinationNames) {
    if (destinationNames == null) {
      return;
    }
    for (String name : destinationNames) {
      if (name == null || name.isBlank()) {
        continue;
      }
      try {
        amqpAdmin.purgeQueue(name, false);
      } catch (Exception ignored) {
        // Queue may not exist yet
      }
    }
  }

  @Override
  public void publish(String destination, byte[] body, Map<String, Object> headers) {
    String payload = body == null ? "" : new String(body, StandardCharsets.UTF_8);
    rabbitTemplate.convertAndSend(
        "",
        destination,
        payload,
        message -> {
          if (headers != null) {
            headers.forEach((k, v) -> message.getMessageProperties().setHeader(k, v));
          }
          return message;
        });
  }

  @Override
  public void deadLetter(String dlqDestination, byte[] body, Map<String, Object> headers) {
    publish(dlqDestination, body, headers);
  }

  @Override
  public long queueDepth(String destination) {
    var info = amqpAdmin.getQueueInfo(destination);
    return info == null ? 0L : info.getMessageCount();
  }

  @Override
  public Map<String, String> pipeletConnectionEnv() {
    Map<String, String> env = new LinkedHashMap<>();
    env.put("IO_BROKER", PlatformAssembly.BROKER_RABBITMQ);
    env.put("AMQP_URL", resolveAmqpUrl());
    return env;
  }

  private String resolveAmqpUrl() {
    if (StringUtils.hasText(amqpUrlOverride)) {
      return amqpUrlOverride.trim();
    }
    String host = rabbitProperties.getHost() == null ? "localhost" : rabbitProperties.getHost();
    int port = rabbitProperties.getPort();
    String user =
        rabbitProperties.getUsername() == null ? "guest" : rabbitProperties.getUsername();
    String pass =
        rabbitProperties.getPassword() == null ? "guest" : rabbitProperties.getPassword();
    String vhost = rabbitProperties.getVirtualHost();
    if (vhost == null || vhost.isBlank() || "/".equals(vhost)) {
      return "amqp://" + user + ":" + pass + "@" + host + ":" + port + "/";
    }
    String encodedVhost = vhost.startsWith("/") ? vhost.substring(1) : vhost;
    return "amqp://" + user + ":" + pass + "@" + host + ":" + port + "/" + encodedVhost;
  }
}
