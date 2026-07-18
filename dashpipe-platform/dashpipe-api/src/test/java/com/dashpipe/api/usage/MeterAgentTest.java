package com.dashpipe.api.usage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** W5-US02: MeterAgent emits records + stub compute (+ pipeline_runs on last stage). */
class MeterAgentTest {

  private StubUsageEventCollector collector;
  private MeterAgent meterAgent;

  @BeforeEach
  void setUp() {
    collector = new StubUsageEventCollector();
    meterAgent = new MeterAgent(collector);
  }

  @Test
  void middleStage_emitsRecordsAndVcpu_notPipelineRuns() {
    meterAgent.recordStageProcessed(
        "T001", "pipe-1", "exec-1", "plet-src", 1, 3, 1L, Duration.ofMillis(5));

    assertThat(collector.getEvents())
        .hasSize(2)
        .anySatisfy(
            e -> {
              assertThat(e.dimension()).isEqualTo(UsageEvent.RECORDS_PROCESSED);
              assertThat(e.amount()).isEqualTo(1.0);
              assertThat(e.tenantId()).isEqualTo("T001");
              assertThat(e.pipelineId()).isEqualTo("pipe-1");
              assertThat(e.executionId()).isEqualTo("exec-1");
              assertThat(e.pipeletId()).isEqualTo("plet-src");
              assertThat(e.unit()).isEqualTo("records");
              assertThat(e.idempotencyKey()).contains("stage:exec-1:1:");
            })
        .anySatisfy(
            e -> {
              assertThat(e.dimension()).isEqualTo(UsageEvent.VCPU_SECONDS);
              assertThat(e.amount()).isEqualTo(MeterAgent.STUB_VCPU_SECONDS_PER_STAGE);
              assertThat(e.unit()).isEqualTo("vcpu_seconds");
            });
    assertThat(collector.getEvents())
        .noneMatch(e -> UsageEvent.PIPELINE_RUNS.equals(e.dimension()));
  }

  @Test
  void lastStage_alsoEmitsPipelineRuns() {
    meterAgent.recordStageProcessed(
        "T001", "pipe-1", "exec-1", "plet-dest", 3, 3, 1L, Duration.ofMillis(1));

    assertThat(collector.getEvents())
        .hasSize(3)
        .anySatisfy(
            e -> {
              assertThat(e.dimension()).isEqualTo(UsageEvent.PIPELINE_RUNS);
              assertThat(e.amount()).isEqualTo(1.0);
              assertThat(e.unit()).isEqualTo("runs");
            });
  }

  @Test
  void zeroRecords_skipsRecordsDimension() {
    meterAgent.recordStageProcessed(
        "T001", "p", "e", "x", 1, 1, 0L, Duration.ofMillis(1));

    assertThat(collector.getEvents())
        .noneMatch(e -> UsageEvent.RECORDS_PROCESSED.equals(e.dimension()))
        .anySatisfy(e -> assertThat(e.dimension()).isEqualTo(UsageEvent.VCPU_SECONDS))
        .anySatisfy(e -> assertThat(e.dimension()).isEqualTo(UsageEvent.PIPELINE_RUNS));
  }

  @Test
  void blankTenant_emitsNothing() {
    meterAgent.recordStageProcessed("  ", "p", "e", "x", 1, 1, 1L, Duration.ofMillis(1));
    assertThat(collector.getEvents()).isEmpty();
  }
}
