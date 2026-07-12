package com.dashflow.api.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

  /** Shared stub consumer queue for Wave 2 stage handoff (real Jobs in W2-US05). */
  public static final String STUB_STAGE_WORKER_QUEUE = "platform.stub.stage.worker";

  @Bean
  public MessageConverter jacksonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public Queue stubStageWorkerQueue() {
    return QueueBuilder.durable(STUB_STAGE_WORKER_QUEUE).build();
  }
}
