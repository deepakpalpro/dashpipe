package com.dashflow.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** W2-US03: declare tenant-prefixed topology and round-trip a message (Compose RabbitMQ). */
@SpringBootTest
@ActiveProfiles("local")
class RabbitTopologyIT {

  @BeforeAll
  static void requireComposeDeps() {
    assumeTrue(
        isPortOpen("127.0.0.1", 3306),
        "Compose MySQL is not reachable on localhost:3306 — run: docker compose up -d mysql");
    assumeTrue(
        isPortOpen("127.0.0.1", 5672),
        "Compose RabbitMQ is not reachable on localhost:5672 — run: docker compose up -d rabbitmq");
  }

  private static boolean isPortOpen(String host, int port) {
    try (Socket socket = new Socket(host, port)) {
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  @Autowired private PipelineTopologyService pipelineTopologyService;
  @Autowired private RabbitTemplate rabbitTemplate;

  @Test
  void declareAndPublish() {
    String tenantId = "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String pipelineId = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    PipelineTopology first = pipelineTopologyService.declare(tenantId, pipelineId, 3);
    PipelineTopology second = pipelineTopologyService.declare(tenantId, pipelineId, 3);

    assertThat(first.exchange()).contains(tenantId).contains(pipelineId);
    assertThat(first.stages()).hasSize(3);
    assertThat(second.exchange()).isEqualTo(first.exchange());
    assertThat(first.stages().get(0).inputQueue())
        .isEqualTo(QueueNaming.stageInputQueue(tenantId, pipelineId, 1));
    assertThat(first.stages().get(0).dlq()).endsWith(".stage.1.dlq");
    assertThat(first.stages().get(0).outputQueue())
        .isEqualTo(QueueNaming.stageInputQueue(tenantId, pipelineId, 2));
    assertThat(first.stages().get(2).outputQueue()).isNull();

    String payload = "hello-" + UUID.randomUUID();
    PipelineStageTopology stage1 = first.stages().get(0);
    rabbitTemplate.convertAndSend(first.exchange(), stage1.routingKey(), payload);

    Object received =
        rabbitTemplate.receiveAndConvert(stage1.inputQueue(), TimeUnit.SECONDS.toMillis(5));
    assertThat(received).isEqualTo(payload);
  }
}
