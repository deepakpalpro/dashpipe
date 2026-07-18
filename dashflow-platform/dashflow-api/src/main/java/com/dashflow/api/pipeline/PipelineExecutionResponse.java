package com.dashflow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonProperty("error_summary") String errorSummary,
    List<StepStatus> steps) {

  /** Per-stage (pipelet) status + timing for an execution. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record StepStatus(
      @JsonProperty("step_order") int stepOrder,
      @JsonProperty("pipelet_id") String pipeletId,
      ExecutionStatus status,
      @JsonProperty("started_at") Instant startedAt,
      @JsonProperty("completed_at") Instant completedAt,
      @JsonProperty("records_in") Long recordsIn,
      @JsonProperty("records_out") Long recordsOut,
      @JsonProperty("error_summary") String errorSummary) {

    static StepStatus from(PipelineExecutionStep step) {
      return new StepStatus(
          step.getStepOrder(),
          step.getPipeletId(),
          step.getStatus(),
          step.getStartedAt(),
          step.getCompletedAt(),
          step.getRecordsIn(),
          step.getRecordsOut(),
          step.getErrorSummary());
    }
  }

  static PipelineExecutionResponse from(PipelineExecution entity) {
    return from(entity, null);
  }

  static PipelineExecutionResponse from(
      PipelineExecution entity, List<PipelineExecutionStep> steps) {
    List<StepStatus> stepStatuses =
        steps == null ? null : steps.stream().map(StepStatus::from).toList();
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
        entity.getErrorSummary(),
        stepStatuses);
  }
}
