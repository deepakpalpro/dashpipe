package com.dashflow.api.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Emits pipelet runtime metrics (architecture §7.1 / §7.5). Wave 4 labels are low-cardinality:
 * {@code tenant_id}, {@code pipeline_id}, {@code pipelet_id} (+ fixed {@code pod_name} /
 * allowlisted {@code error_type}). {@code execution_id} is intentionally omitted from these series
 * to avoid unbounded Prometheus cardinality.
 */
@Component
public class PipeletMetricsEmitter {

  public static final String RECORDS_IN = "pipelet_records_in_total";
  public static final String RECORDS_OUT = "pipelet_records_out_total";
  public static final String PROCESSING_DURATION = "pipelet_processing_duration_seconds";
  public static final String HEARTBEAT_TIMESTAMP = "pipelet_heartbeat_timestamp";
  public static final String ERRORS_TOTAL = "pipelet_errors_total";

  public static final String TAG_TENANT = "tenant_id";
  public static final String TAG_PIPELINE = "pipeline_id";
  public static final String TAG_PIPELET = "pipelet_id";
  public static final String TAG_POD = "pod_name";
  public static final String TAG_ERROR_TYPE = "error_type";

  /** Fixed stub pod label for Wave 4 (avoids unbounded {@code pod_name} cardinality). */
  public static final String STUB_POD_NAME = "stub-pipelet";

  private final MeterRegistry meterRegistry;
  private final ConcurrentHashMap<String, AtomicLong> heartbeats = new ConcurrentHashMap<>();

  public PipeletMetricsEmitter(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordIn(String tenantId, String pipelineId, String pipeletId, long count) {
    if (count <= 0) {
      return;
    }
    counter(RECORDS_IN, tenantId, pipelineId, pipeletId).increment(count);
  }

  public void recordOut(String tenantId, String pipelineId, String pipeletId, long count) {
    if (count <= 0) {
      return;
    }
    counter(RECORDS_OUT, tenantId, pipelineId, pipeletId).increment(count);
  }

  public void recordProcessing(
      String tenantId, String pipelineId, String pipeletId, Duration duration) {
    if (duration == null || duration.isNegative()) {
      return;
    }
    timer(tenantId, pipelineId, pipeletId).record(duration);
  }

  /**
   * Convenience for stub/local processing: one batch with matching in/out and a measured duration.
   */
  public void recordBatch(
      String tenantId,
      String pipelineId,
      String pipeletId,
      long recordsIn,
      long recordsOut,
      Duration duration) {
    recordIn(tenantId, pipelineId, pipeletId, recordsIn);
    recordOut(tenantId, pipelineId, pipeletId, recordsOut);
    recordProcessing(tenantId, pipelineId, pipeletId, duration);
  }

  /** Sets {@code pipelet_heartbeat_timestamp} to current epoch seconds (architecture §7.5). */
  public void touchHeartbeat(String tenantId, String pipelineId, String pipeletId) {
    touchHeartbeat(tenantId, pipelineId, pipeletId, STUB_POD_NAME, Instant.now().getEpochSecond());
  }

  public void touchHeartbeat(
      String tenantId, String pipelineId, String pipeletId, String podName, long epochSeconds) {
    String tenant = nullToUnknown(tenantId);
    String pipeline = nullToUnknown(pipelineId);
    String pipelet = nullToUnknown(pipeletId);
    String pod = nullToUnknown(podName);
    String key = tenant + ":" + pipeline + ":" + pipelet + ":" + pod;
    AtomicLong holder =
        heartbeats.computeIfAbsent(
            key,
            k -> {
              AtomicLong ref = new AtomicLong(0L);
              Gauge.builder(HEARTBEAT_TIMESTAMP, ref, AtomicLong::doubleValue)
                  .tags(
                      Tags.of(
                          TAG_TENANT,
                          tenant,
                          TAG_PIPELINE,
                          pipeline,
                          TAG_PIPELET,
                          pipelet,
                          TAG_POD,
                          pod))
                  .register(meterRegistry);
              return ref;
            });
    holder.set(Math.max(0L, epochSeconds));
  }

  /** Increments {@code pipelet_errors_total} for an allowlisted critical {@code error_type}. */
  public void recordCriticalError(
      String tenantId, String pipelineId, String pipeletId, PipeletErrorType errorType) {
    PipeletErrorType type = errorType == null ? PipeletErrorType.UNKNOWN : errorType;
    Counter.builder(ERRORS_TOTAL)
        .tag(TAG_TENANT, nullToUnknown(tenantId))
        .tag(TAG_PIPELINE, nullToUnknown(pipelineId))
        .tag(TAG_PIPELET, nullToUnknown(pipeletId))
        .tag(TAG_ERROR_TYPE, type.prometheusLabel())
        .register(meterRegistry)
        .increment();
  }

  private Counter counter(String name, String tenantId, String pipelineId, String pipeletId) {
    return Counter.builder(name)
        .tag(TAG_TENANT, nullToUnknown(tenantId))
        .tag(TAG_PIPELINE, nullToUnknown(pipelineId))
        .tag(TAG_PIPELET, nullToUnknown(pipeletId))
        .register(meterRegistry);
  }

  private Timer timer(String tenantId, String pipelineId, String pipeletId) {
    return Timer.builder(PROCESSING_DURATION)
        .tag(TAG_TENANT, nullToUnknown(tenantId))
        .tag(TAG_PIPELINE, nullToUnknown(pipelineId))
        .tag(TAG_PIPELET, nullToUnknown(pipeletId))
        .register(meterRegistry);
  }

  private static String nullToUnknown(String value) {
    return value == null || value.isBlank() ? "unknown" : value;
  }
}
