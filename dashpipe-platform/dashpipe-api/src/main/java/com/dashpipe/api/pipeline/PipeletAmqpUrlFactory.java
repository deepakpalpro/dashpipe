package com.dashpipe.api.pipeline;

import com.dashpipe.spi.messaging.MessageBroker;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves pipelet broker connection settings from the active {@link MessageBroker} assembly.
 *
 * <p>Retains {@code AMQP_URL} resolution for RabbitMQ assemblies; Service Bus assemblies expose
 * {@code SERVICEBUS_*} via {@link MessageBroker#pipeletConnectionEnv()}.
 */
@Component
public class PipeletAmqpUrlFactory {

  private final MessageBroker messageBroker;
  private final PipelineOrchestrationProperties orchestrationProperties;

  public PipeletAmqpUrlFactory(
      MessageBroker messageBroker, PipelineOrchestrationProperties orchestrationProperties) {
    this.messageBroker = messageBroker;
    this.orchestrationProperties = orchestrationProperties;
  }

  public String resolve() {
    if (StringUtils.hasText(orchestrationProperties.getAmqpUrl())) {
      return orchestrationProperties.getAmqpUrl().trim();
    }
    Map<String, String> env = messageBroker.pipeletConnectionEnv();
    String amqp = env.get("AMQP_URL");
    return amqp == null ? "" : amqp;
  }

  public Map<String, String> connectionEnv() {
    Map<String, String> env = new java.util.LinkedHashMap<>(messageBroker.pipeletConnectionEnv());
    if (StringUtils.hasText(orchestrationProperties.getAmqpUrl())) {
      env.put("AMQP_URL", orchestrationProperties.getAmqpUrl().trim());
    }
    return env;
  }
}
