package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Stage handoff payload published on RabbitMQ between pipeline stages. */
public record StageMessage(
    @JsonProperty("execution_id") String executionId,
    @JsonProperty("pipeline_id") String pipelineId,
    @JsonProperty("tenant_id") String tenantId,
    @JsonProperty("pipelet_id") String pipeletId,
    @JsonProperty("stage_order") int stageOrder,
    @JsonProperty("stage_count") int stageCount,
    String payload,
    @JsonProperty("io_mode") String ioMode,
    @JsonProperty("amqp_url") String amqpUrl) {

  /** Backward-compatible constructor used by older call sites / tests. */
  public StageMessage(
      String executionId,
      String pipelineId,
      String tenantId,
      String pipeletId,
      int stageOrder,
      int stageCount,
      String payload) {
    this(
        executionId,
        pipelineId,
        tenantId,
        pipeletId,
        stageOrder,
        stageCount,
        payload,
        PipelineIoMode.DEFAULT,
        null);
  }
}
