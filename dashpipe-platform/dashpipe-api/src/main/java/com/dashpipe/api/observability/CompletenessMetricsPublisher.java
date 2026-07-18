package com.dashpipe.api.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code pipeline_completeness_ratio} (architecture §7.1 / §7.4). Labels are {@code
 * tenant_id} + {@code pipeline_id} only (latest ratio per pipeline) to avoid unbounded {@code
 * execution_id} cardinality. Per-execution percent is stored on {@code pipeline_executions}.
 */
@Component
public class CompletenessMetricsPublisher {

  public static final String COMPLETENESS_RATIO = "pipeline_completeness_ratio";

  private final MeterRegistry meterRegistry;
  private final ConcurrentHashMap<String, AtomicReference<Double>> latest = new ConcurrentHashMap<>();

  public CompletenessMetricsPublisher(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void publish(String tenantId, String pipelineId, double ratio) {
    String tenant = nullToUnknown(tenantId);
    String pipeline = nullToUnknown(pipelineId);
    String key = tenant + ":" + pipeline;
    AtomicReference<Double> holder =
        latest.computeIfAbsent(
            key,
            k -> {
              AtomicReference<Double> ref = new AtomicReference<>(0.0);
              Gauge.builder(COMPLETENESS_RATIO, ref, AtomicReference::get)
                  .tags(
                      Tags.of(
                          PipeletMetricsEmitter.TAG_TENANT,
                          tenant,
                          PipeletMetricsEmitter.TAG_PIPELINE,
                          pipeline))
                  .register(meterRegistry);
              return ref;
            });
    holder.set(clamp(ratio));
  }

  private static double clamp(double ratio) {
    if (Double.isNaN(ratio) || ratio < 0.0) {
      return 0.0;
    }
    return Math.min(ratio, 1.0);
  }

  private static String nullToUnknown(String value) {
    return value == null || value.isBlank() ? "unknown" : value;
  }
}
