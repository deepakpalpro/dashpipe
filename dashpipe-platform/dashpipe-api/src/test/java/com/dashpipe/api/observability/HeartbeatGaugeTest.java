package com.dashpipe.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** W4-US03: pipelet_heartbeat_timestamp gauge (epoch seconds). */
class HeartbeatGaugeTest {

  private SimpleMeterRegistry registry;
  private PipeletMetricsEmitter emitter;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    emitter = new PipeletMetricsEmitter(registry);
  }

  @Test
  void touchHeartbeat_setsEpochSecondsGauge() {
    emitter.touchHeartbeat("T001", "pipe-1", "plet-src", PipeletMetricsEmitter.STUB_POD_NAME, 1_700_000_000L);

    Double value =
        registry
            .find(PipeletMetricsEmitter.HEARTBEAT_TIMESTAMP)
            .tag(PipeletMetricsEmitter.TAG_TENANT, "T001")
            .tag(PipeletMetricsEmitter.TAG_PIPELINE, "pipe-1")
            .tag(PipeletMetricsEmitter.TAG_PIPELET, "plet-src")
            .tag(PipeletMetricsEmitter.TAG_POD, PipeletMetricsEmitter.STUB_POD_NAME)
            .gauge()
            .value();
    assertThat(value).isEqualTo(1_700_000_000.0);
  }

  @Test
  void touchHeartbeat_updatesSameSeries() {
    emitter.touchHeartbeat("T001", "p", "x", PipeletMetricsEmitter.STUB_POD_NAME, 100L);
    emitter.touchHeartbeat("T001", "p", "x", PipeletMetricsEmitter.STUB_POD_NAME, 200L);

    Double value =
        registry
            .find(PipeletMetricsEmitter.HEARTBEAT_TIMESTAMP)
            .tag(PipeletMetricsEmitter.TAG_TENANT, "T001")
            .tag(PipeletMetricsEmitter.TAG_PIPELINE, "p")
            .tag(PipeletMetricsEmitter.TAG_PIPELET, "x")
            .tag(PipeletMetricsEmitter.TAG_POD, PipeletMetricsEmitter.STUB_POD_NAME)
            .gauge()
            .value();
    assertThat(value).isEqualTo(200.0);
  }
}
