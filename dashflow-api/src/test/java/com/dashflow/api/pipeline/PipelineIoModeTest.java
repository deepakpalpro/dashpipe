package com.dashflow.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PipelineIoModeTest {

  @Test
  void defaultsToQueueWhenMissing() {
    assertThat(PipelineIoMode.fromExecutionConfigJson(null)).isEqualTo(PipelineIoMode.QUEUE);
    assertThat(PipelineIoMode.fromExecutionConfigJson("")).isEqualTo(PipelineIoMode.QUEUE);
    assertThat(PipelineIoMode.fromExecutionConfigJson("{}")).isEqualTo(PipelineIoMode.QUEUE);
    assertThat(PipelineIoMode.fromExecutionConfigJson("{not-json")).isEqualTo(PipelineIoMode.QUEUE);
  }

  @Test
  void readsIoMode() {
    assertThat(PipelineIoMode.fromExecutionConfigJson("{\"ioMode\":\"stdio\"}"))
        .isEqualTo(PipelineIoMode.STDIO);
    assertThat(PipelineIoMode.fromExecutionConfigJson("{\"ioMode\":\"QUEUE\"}"))
        .isEqualTo(PipelineIoMode.QUEUE);
    assertThat(PipelineIoMode.fromExecutionConfigJson("{\"io_mode\":\"stdio\"}"))
        .isEqualTo(PipelineIoMode.STDIO);
  }

  @Test
  void unknownFallsBackToQueue() {
    assertThat(PipelineIoMode.fromExecutionConfigJson("{\"ioMode\":\"kafka\"}"))
        .isEqualTo(PipelineIoMode.QUEUE);
  }
}
