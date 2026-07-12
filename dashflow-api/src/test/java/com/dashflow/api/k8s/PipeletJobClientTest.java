package com.dashflow.api.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PipeletJobClientTest {

  private StubPipeletJobClient client;

  @BeforeEach
  void setUp() {
    client = new StubPipeletJobClient();
  }

  @Test
  void stub_recordsCreate() {
    PipeletJobRequest request =
        PipeletJobRequest.of(
            "T001",
            "pipe-abc",
            "exec-1",
            "plet-rest-source",
            1,
            3,
            "tenant.T001.pipeline.pipe-abc.stage.1.in",
            "tenant.T001.pipeline.pipe-abc.stage.2.in");

    PipeletJobHandle handle = client.create(request);

    assertThat(handle.jobName()).isEqualTo("exec-exec-1-stage-1");
    assertThat(handle.namespace()).isEqualTo("tenant-t001");
    assertThat(handle.status()).isEqualTo("stubbed");
    assertThat(client.getCreated()).hasSize(1);
    PipeletJobRequest recorded = client.getCreated().get(0);
    assertThat(recorded.tenantId()).isEqualTo("T001");
    assertThat(recorded.pipelineId()).isEqualTo("pipe-abc");
    assertThat(recorded.executionId()).isEqualTo("exec-1");
    assertThat(recorded.pipeletId()).isEqualTo("plet-rest-source");
    assertThat(recorded.stageOrder()).isEqualTo(1);
    assertThat(recorded.inputQueue()).contains("T001").endsWith(".stage.1.in");
    assertThat(recorded.ioMode()).isEqualTo("queue");
  }

  @Test
  void stub_deleteByExecutionRemovesMatchingCreates() {
    client.create(
        PipeletJobRequest.of("T001", "pipe-abc", "exec-1", "plet-a", 1, 2, "q1", "q2"));
    client.create(
        PipeletJobRequest.of("T001", "pipe-abc", "exec-2", "plet-a", 1, 2, "q1", "q2"));

    assertThat(client.deleteByExecution("T001", "exec-1")).isEqualTo(1);
    assertThat(client.getCreated()).hasSize(1);
    assertThat(client.getCreated().get(0).executionId()).isEqualTo("exec-2");
    assertThat(client.getDeletedExecutionIds()).containsExactly("exec-1");
  }

  @Test
  void stub_recordsIoModeAndAmqpUrl() {
    PipeletJobRequest request =
        PipeletJobRequest.of(
            "T001",
            "pipe-abc",
            "exec-1",
            "plet-csv-to-json",
            1,
            2,
            "q.in",
            "q.out",
            "stdio",
            "amqp://pipeline:pipeline@localhost:5672/");

    client.create(request);

    PipeletJobRequest recorded = client.getCreated().get(0);
    assertThat(recorded.ioMode()).isEqualTo("stdio");
    assertThat(recorded.amqpUrl()).contains("localhost:5672");
    assertThat(recorded.outputQueue()).isEqualTo("q.out");
  }

  @Test
  void stub_rejectsNullRequest() {
    assertThatThrownBy(() -> client.create(null)).isInstanceOf(IllegalArgumentException.class);
  }
}
