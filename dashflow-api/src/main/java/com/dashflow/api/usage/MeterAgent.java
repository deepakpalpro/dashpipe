package com.dashflow.api.usage;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emits billable usage for pipelet/stub stage processing (architecture §6.2). Distinct from
 * Prometheus pipelet metrics (W4) — this path feeds {@link UsageEventCollector} / MySQL.
 */
@Component
public class MeterAgent {

  private static final Logger log = LoggerFactory.getLogger(MeterAgent.class);

  /** Fixed stub compute charge per stage (Wave 5 fixture; not real K8s metrics-server). */
  public static final double STUB_VCPU_SECONDS_PER_STAGE = 0.001;

  private final UsageEventCollector collector;

  public MeterAgent(UsageEventCollector collector) {
    this.collector = collector;
  }

  /**
   * Record usage for one completed stub/pipelet stage. Emits {@code data.records_processed} and
   * stub {@code compute.vcpu_seconds}. On the last stage also emits {@code platform.pipeline_runs}
   * = 1.
   */
  public void recordStageProcessed(
      String tenantId,
      String pipelineId,
      String executionId,
      String pipeletId,
      int stageOrder,
      int stageCount,
      long recordsProcessed,
      Duration processingDuration) {
    if (tenantId == null || tenantId.isBlank()) {
      return;
    }
    Instant now = Instant.now();
    try {
      if (recordsProcessed > 0) {
        collect(
            stageEvent(
                UsageEvent.RECORDS_PROCESSED,
                recordsProcessed,
                "records",
                tenantId,
                pipelineId,
                executionId,
                pipeletId,
                stageOrder,
                now));
      }
      double vcpu = MeterAgent.STUB_VCPU_SECONDS_PER_STAGE;
      collect(
          stageEvent(
              UsageEvent.VCPU_SECONDS,
              vcpu,
              "vcpu_seconds",
              tenantId,
              pipelineId,
              executionId,
              pipeletId,
              stageOrder,
              now));

      if (stageOrder >= stageCount && stageCount > 0) {
        collect(
            stageEvent(
                UsageEvent.PIPELINE_RUNS,
                1.0,
                "runs",
                tenantId,
                pipelineId,
                executionId,
                pipeletId,
                stageOrder,
                now));
      }
    } catch (Exception ex) {
      log.warn(
          "MeterAgent failed tenant={} execution={} stage={}/{}: {}",
          tenantId,
          executionId,
          stageOrder,
          stageCount,
          ex.toString());
    }
  }

  private void collect(UsageEvent event) {
    collector.collect(event);
  }

  private static UsageEvent stageEvent(
      String dimension,
      double amount,
      String unit,
      String tenantId,
      String pipelineId,
      String executionId,
      String pipeletId,
      int stageOrder,
      Instant when) {
    String key =
        "stage:"
            + nullToEmpty(executionId)
            + ":"
            + stageOrder
            + ":"
            + dimension
            + ":"
            + when.toEpochMilli();
    return new UsageEvent(
            dimension,
            amount,
            tenantId,
            null,
            when,
            executionId,
            pipelineId,
            pipeletId,
            unit,
            key);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
