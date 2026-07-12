package com.dashflow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record PipelineExecutionResponse(
    String id,
    @JsonProperty("pipeline_id") String pipelineId,
    @JsonProperty("tenant_id") String tenantId,
    @JsonProperty("pipeline_version") int pipelineVersion,
    ExecutionStatus status,
    ExecutionTrigger trigger,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("records_in") long recordsIn,
    @JsonProperty("records_out") long recordsOut,
    @JsonProperty("completeness_pct") BigDecimal completenessPct,
    @JsonProperty("error_summary") String errorSummary) {

  static PipelineExecutionResponse from(PipelineExecution entity) {
    return new PipelineExecutionResponse(
        entity.getId(),
        entity.getPipelineId(),
        entity.getTenantId(),
        entity.getPipelineVersion(),
        entity.getStatus(),
        entity.getTrigger(),
        entity.getStartedAt(),
        entity.getCompletedAt(),
        entity.getRecordsIn(),
        entity.getRecordsOut(),
        entity.getCompletenessPct(),
        entity.getErrorSummary());
  }
}
