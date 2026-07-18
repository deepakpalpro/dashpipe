package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record PipelineRunResponse(
    @JsonProperty("execution_id") String executionId,
    ExecutionStatus status,
    @JsonProperty("pipeline_id") String pipelineId,
    @JsonProperty("started_at") Instant startedAt) {

  static PipelineRunResponse from(PipelineExecution execution) {
    return new PipelineRunResponse(
        execution.getId(),
        execution.getStatus(),
        execution.getPipelineId(),
        execution.getStartedAt());
  }
}
