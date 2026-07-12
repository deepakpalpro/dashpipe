package com.dashflow.api.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** W4-US02: pipeline_completeness_ratio gauge. */
class CompletenessMetricsPublisherTest {

  private MeterRegistry registry;
  private CompletenessMetricsPublisher publisher;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    publisher = new CompletenessMetricsPublisher(registry);
  }

  @Test
  void publish_registersGaugeWithTenantAndPipelineLabels() {
    publisher.publish("tenant-a", "p1", 0.98);

    Double value =
        registry
            .find(CompletenessMetricsPublisher.COMPLETENESS_RATIO)
            .tag(PipeletMetricsEmitter.TAG_TENANT, "tenant-a")
            .tag(PipeletMetricsEmitter.TAG_PIPELINE, "p1")
            .gauge()
            .value();
    assertThat(value).isCloseTo(0.98, within(1e-9));
  }

  @Test
  void publish_updatesLatestPerPipeline() {
    publisher.publish("tenant-a", "p1", 0.5);
    publisher.publish("tenant-a", "p1", 1.0);

    Double value =
        registry
            .find(CompletenessMetricsPublisher.COMPLETENESS_RATIO)
            .tag(PipeletMetricsEmitter.TAG_TENANT, "tenant-a")
            .tag(PipeletMetricsEmitter.TAG_PIPELINE, "p1")
            .gauge()
            .value();
    assertThat(value).isEqualTo(1.0);
  }
}
