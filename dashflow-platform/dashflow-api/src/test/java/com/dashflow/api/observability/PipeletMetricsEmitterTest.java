package com.dashflow.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** W4-US01: pipelet counters + processing histogram. */
class PipeletMetricsEmitterTest {

  private SimpleMeterRegistry registry;
  private PipeletMetricsEmitter emitter;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    emitter = new PipeletMetricsEmitter(registry);
  }

  @Test
  void emit_incrementsCounters() {
    emitter.recordBatch("T001", "pipe-1", "plet-src", 10, 9, Duration.ofMillis(5));

    assertThat(
            registry
                .counter(
                    PipeletMetricsEmitter.RECORDS_IN,
                    PipeletMetricsEmitter.TAG_TENANT,
                    "T001",
                    PipeletMetricsEmitter.TAG_PIPELINE,
                    "pipe-1",
                    PipeletMetricsEmitter.TAG_PIPELET,
                    "plet-src")
                .count())
        .isEqualTo(10.0);
    assertThat(
            registry
                .counter(
                    PipeletMetricsEmitter.RECORDS_OUT,
                    PipeletMetricsEmitter.TAG_TENANT,
                    "T001",
                    PipeletMetricsEmitter.TAG_PIPELINE,
                    "pipe-1",
                    PipeletMetricsEmitter.TAG_PIPELET,
                    "plet-src")
                .count())
        .isEqualTo(9.0);
    assertThat(
            registry
                .timer(
                    PipeletMetricsEmitter.PROCESSING_DURATION,
                    PipeletMetricsEmitter.TAG_TENANT,
                    "T001",
                    PipeletMetricsEmitter.TAG_PIPELINE,
                    "pipe-1",
                    PipeletMetricsEmitter.TAG_PIPELET,
                    "plet-src")
                .count())
        .isEqualTo(1L);
  }

  @Test
  void zeroCounts_doNotRegisterIncrements() {
    emitter.recordIn("T001", "p", "x", 0);
    emitter.recordOut("T001", "p", "x", -1);
    assertThat(registry.find(PipeletMetricsEmitter.RECORDS_IN).counters()).isEmpty();
    assertThat(registry.find(PipeletMetricsEmitter.RECORDS_OUT).counters()).isEmpty();
  }
}
