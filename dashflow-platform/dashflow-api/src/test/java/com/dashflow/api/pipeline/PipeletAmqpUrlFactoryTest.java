package com.dashflow.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dashflow.spi.messaging.MessageBroker;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PipeletAmqpUrlFactoryTest {

  @Mock private MessageBroker messageBroker;

  @Test
  void buildsFromBrokerConnectionEnv() {
    when(messageBroker.pipeletConnectionEnv())
        .thenReturn(Map.of("IO_BROKER", "rabbitmq", "AMQP_URL", "amqp://pipeline:pipeline@rabbitmq:5672/"));
    PipelineOrchestrationProperties orch = new PipelineOrchestrationProperties();
    PipeletAmqpUrlFactory factory = new PipeletAmqpUrlFactory(messageBroker, orch);

    assertThat(factory.resolve()).isEqualTo("amqp://pipeline:pipeline@rabbitmq:5672/");
  }

  @Test
  void prefersExplicitOverride() {
    PipelineOrchestrationProperties orch = new PipelineOrchestrationProperties();
    orch.setAmqpUrl("amqp://custom@broker:5672/vhost");
    PipeletAmqpUrlFactory factory = new PipeletAmqpUrlFactory(messageBroker, orch);

    assertThat(factory.resolve()).isEqualTo("amqp://custom@broker:5672/vhost");
  }
}
