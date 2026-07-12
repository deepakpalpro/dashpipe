package com.dashflow.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QueueNamingTest {

  @Test
  void includesTenantId() {
    String exchange = QueueNaming.pipelineExchange("T001", "pipe-abc");
    String input = QueueNaming.stageInputQueue("T001", "pipe-abc", 1);
    String dlq = QueueNaming.stageDlq("T001", "pipe-abc", 2);
    String webhook = QueueNaming.webhookInputQueue("T001", "conn-github");

    assertThat(exchange).isEqualTo("tenant.T001.pipeline.pipe-abc");
    assertThat(input).contains("T001").isEqualTo("tenant.T001.pipeline.pipe-abc.stage.1.in");
    assertThat(dlq).contains("T001").endsWith(".stage.2.dlq");
    assertThat(webhook).contains("T001").isEqualTo("tenant.T001.webhook.conn-github.in");
    assertThat(QueueNaming.webhookRoutingKey("conn-github")).isEqualTo("conn-github");
    assertThat(QueueNaming.webhookExchange("T001")).isEqualTo("tenant.T001.webhook");
    assertThat(QueueNaming.webhookDlq("T001", "conn-github"))
        .isEqualTo("tenant.T001.webhook.conn-github.dlq");
    assertThat(QueueNaming.stageRoutingKey(3)).isEqualTo("stage.3");
    assertThat(QueueNaming.stageOutputQueue("T001", "pipe-abc", 1))
        .isEqualTo(QueueNaming.stageInputQueue("T001", "pipe-abc", 2));
    assertThat(QueueNaming.deadLetterExchange("T001", "pipe-abc"))
        .isEqualTo("tenant.T001.pipeline.pipe-abc.dlx");
    assertThat(QueueNaming.stageDlqRoutingKey(1)).isEqualTo("stage.1.dlq");
  }

  @Test
  void rejectsBlankOrUnsafeTokens() {
    assertThatThrownBy(() -> QueueNaming.pipelineExchange(" ", "p1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> QueueNaming.pipelineExchange("t.a", "p1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> QueueNaming.stageInputQueue("t1", "p1", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
