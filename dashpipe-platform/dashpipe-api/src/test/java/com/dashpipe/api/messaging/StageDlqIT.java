package com.dashpipe.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.Socket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** W2-US06: poison messages land on per-stage DLQ after retries exhaust. */
@SpringBootTest
@ActiveProfiles("local")
class StageDlqIT {

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
  @Autowired private StageDeadLetterService stageDeadLetterService;
  @Autowired private RabbitTemplate rabbitTemplate;

  @Test
  void poison_landsOnDlq() {
    String tenantId = "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String pipelineId = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    PipelineTopology topology = pipelineTopologyService.declare(tenantId, pipelineId, 1);
    PipelineStageTopology stage1 = topology.stages().get(0);

    String poison = "poison-" + UUID.randomUUID();
    RetryPolicy policy = new RetryPolicy(2, 2.0, 1L, 10L);

    // Exhaust retries: first failure republishes to stage queue; second dead-letters.
    assertThat(
            stageDeadLetterService.handleFailure(
                tenantId, pipelineId, 1, poison, 0, policy, "forced-failure"))
        .isFalse();
    assertThat(
            stageDeadLetterService.handleFailure(
                tenantId, pipelineId, 1, poison, 1, policy, "forced-failure"))
        .isTrue();

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              Message fromDlq =
                  rabbitTemplate.receive(stage1.dlq(), TimeUnit.SECONDS.toMillis(1));
              assertThat(fromDlq).isNotNull();
              Object body = rabbitTemplate.getMessageConverter().fromMessage(fromDlq);
              assertThat(body).isEqualTo(poison);
              assertThat(fromDlq.getMessageProperties().getHeaders())
                  .containsEntry(StageDeadLetterService.HEADER_FAILURE_COUNT, 2);
              assertThat(fromDlq.getMessageProperties().getHeaders())
                  .containsEntry(StageDeadLetterService.HEADER_ERROR, "forced-failure");
            });

    assertThat(stage1.dlq()).contains(tenantId).endsWith(".stage.1.dlq");
  }
}
