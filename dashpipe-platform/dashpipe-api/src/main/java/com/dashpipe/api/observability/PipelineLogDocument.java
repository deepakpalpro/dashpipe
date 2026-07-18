package com.dashpipe.api.observability;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Structured pipeline log document (architecture §7.3). Indexed under {@link
 * PipelineLogIndexNames}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PipelineLogDocument(
    @JsonProperty("@timestamp") Instant timestamp,
    String level,
    @JsonProperty("tenant_id") String tenantId,
    @JsonProperty("pipeline_id") String pipelineId,
    @JsonProperty("execution_id") String executionId,
    @JsonProperty("pipelet_id") String pipeletId,
    @JsonProperty("pod_name") String podName,
    String message,
    @JsonProperty("records_in") Long recordsIn,
    @JsonProperty("records_out") Long recordsOut,
    @JsonProperty("duration_ms") Long durationMs) {

  public static PipelineLogDocument info(
      String tenantId,
      String pipelineId,
      String executionId,
      String pipeletId,
      String message,
      long recordsIn,
      long recordsOut,
      long durationMs) {
    return new PipelineLogDocument(
        Instant.now(),
        "INFO",
        tenantId,
        pipelineId,
        executionId,
        pipeletId,
        PipeletMetricsEmitter.STUB_POD_NAME,
        message,
        recordsIn,
        recordsOut,
        durationMs);
  }

  public static PipelineLogDocument error(
      String tenantId, String pipelineId, String executionId, String pipeletId, String message) {
    return new PipelineLogDocument(
        Instant.now(),
        "ERROR",
        tenantId,
        pipelineId,
        executionId,
        pipeletId,
        PipeletMetricsEmitter.STUB_POD_NAME,
        message,
        null,
        null,
        null);
  }
}
