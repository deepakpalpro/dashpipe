package com.dashpipe.api.observability;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ObservabilityDtos {

  private ObservabilityDtos() {}

  public record CompletenessResponse(
      @JsonProperty("pipeline_id") String pipelineId,
      @JsonProperty("tenant_id") String tenantId,
      @JsonProperty("execution_id") String executionId,
      @JsonProperty("records_in") long recordsIn,
      @JsonProperty("records_out") long recordsOut,
      @JsonProperty("completeness_pct") BigDecimal completenessPct,
      @JsonProperty("completeness_ratio") double completenessRatio) {}

  public record LatencyResponse(
      @JsonProperty("pipeline_id") String pipelineId,
      @JsonProperty("tenant_id") String tenantId,
      @JsonProperty("sample_count") long sampleCount,
      @JsonProperty("mean_ms") double meanMs,
      @JsonProperty("max_ms") double maxMs) {}

  public record HeartbeatResponse(
      @JsonProperty("pipeline_id") String pipelineId,
      @JsonProperty("tenant_id") String tenantId,
      @JsonProperty("last_heartbeat_epoch_seconds") Long lastHeartbeatEpochSeconds,
      @JsonProperty("stale") boolean stale) {}

  public record ErrorSummaryResponse(
      @JsonProperty("pipeline_id") String pipelineId,
      @JsonProperty("tenant_id") String tenantId,
      @JsonProperty("total_errors") double totalErrors,
      @JsonProperty("by_type") List<ErrorTypeCount> byType) {}

  public record ErrorTypeCount(
      @JsonProperty("error_type") String errorType, double count) {}

  public record ExecutionLogsResponse(
      @JsonProperty("execution_id") String executionId,
      @JsonProperty("tenant_id") String tenantId,
      @JsonProperty("pipeline_id") String pipelineId,
      List<LogEntry> logs) {}

  public record LogEntry(
      @JsonProperty("@timestamp") Instant timestamp,
      String level,
      @JsonProperty("pipelet_id") String pipeletId,
      @JsonProperty("pod_name") String podName,
      String message,
      @JsonProperty("records_in") Long recordsIn,
      @JsonProperty("records_out") Long recordsOut,
      @JsonProperty("duration_ms") Long durationMs) {}

  /** External tool links for the portal (null URLs when not configured for this deployment). */
  public record PortalLinksResponse(
      @JsonProperty("grafana_enabled") boolean grafanaEnabled,
      @JsonProperty("grafana_url") String grafanaUrl,
      @JsonProperty("grafana_label") String grafanaLabel,
      @JsonProperty("elasticsearch_enabled") boolean elasticsearchEnabled,
      @JsonProperty("elasticsearch_url") String elasticsearchUrl,
      @JsonProperty("elasticsearch_label") String elasticsearchLabel) {}
}
