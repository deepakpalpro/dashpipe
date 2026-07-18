package com.dashpipe.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** W4-US03: pipelet_errors_total critical counter. */
class CriticalErrorCounterTest {

  private SimpleMeterRegistry registry;
  private PipeletMetricsEmitter emitter;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    emitter = new PipeletMetricsEmitter(registry);
  }

  @Test
  void recordCriticalError_incrementsAllowlistedType() {
    emitter.recordCriticalError("T001", "pipe-1", "plet-src", PipeletErrorType.PROCESSING);
    emitter.recordCriticalError("T001", "pipe-1", "plet-src", PipeletErrorType.PROCESSING);

    assertThat(
            registry
                .counter(
                    PipeletMetricsEmitter.ERRORS_TOTAL,
                    PipeletMetricsEmitter.TAG_TENANT,
                    "T001",
                    PipeletMetricsEmitter.TAG_PIPELINE,
                    "pipe-1",
                    PipeletMetricsEmitter.TAG_PIPELET,
                    "plet-src",
                    PipeletMetricsEmitter.TAG_ERROR_TYPE,
                    "processing")
                .count())
        .isEqualTo(2.0);
  }

  @Test
  void nullErrorType_mapsToUnknown() {
    emitter.recordCriticalError("T001", "p", "x", null);

    assertThat(
            registry
                .counter(
                    PipeletMetricsEmitter.ERRORS_TOTAL,
                    PipeletMetricsEmitter.TAG_TENANT,
                    "T001",
                    PipeletMetricsEmitter.TAG_PIPELINE,
                    "p",
                    PipeletMetricsEmitter.TAG_PIPELET,
                    "x",
                    PipeletMetricsEmitter.TAG_ERROR_TYPE,
                    "unknown")
                .count())
        .isEqualTo(1.0);
  }
}
