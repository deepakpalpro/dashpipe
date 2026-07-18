package com.dashpipe.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

class PipelineSchedulePollerTest {

  @Test
  void parsesClassicFiveFieldCron() {
    CronExpression expr = PipelineSchedulePoller.parseCron("*/15 * * * *");
    assertThat(expr).isNotNull();
    assertThat(PipelineSchedulePoller.parseCron("*/1 * * * *")).isNotNull();
    assertThat(PipelineSchedulePoller.parseCron("* * * * *")).isNotNull();
  }

  @Test
  void matchesEveryMinuteAndFifteen() {
    Instant at0000 = Instant.parse("2026-07-18T00:00:00Z").truncatedTo(ChronoUnit.MINUTES);
    Instant at0015 = Instant.parse("2026-07-18T00:15:00Z").truncatedTo(ChronoUnit.MINUTES);
    Instant at0007 = Instant.parse("2026-07-18T00:07:00Z").truncatedTo(ChronoUnit.MINUTES);

    assertThat(PipelineSchedulePoller.matchesCurrentMinute("*/15 * * * *", at0000)).isTrue();
    assertThat(PipelineSchedulePoller.matchesCurrentMinute("*/15 * * * *", at0015)).isTrue();
    assertThat(PipelineSchedulePoller.matchesCurrentMinute("*/15 * * * *", at0007)).isFalse();
    assertThat(PipelineSchedulePoller.matchesCurrentMinute("*/1 * * * *", at0007)).isTrue();
    assertThat(PipelineSchedulePoller.matchesCurrentMinute("* * * * *", at0007)).isTrue();
  }

  @Test
  void resolvesCronFromExecutionConfigJson() {
    assertThat(
            PipelineSchedulePoller.cronFromJson(
                "{\"ioMode\":\"queue\",\"scheduleCron\":\"*/1 * * * *\"}"))
        .isEqualTo("*/1 * * * *");
    assertThat(PipelineSchedulePoller.cronFromJson("{\"cron\":\"*/5 * * * *\"}"))
        .isEqualTo("*/5 * * * *");
  }

  @Test
  void resolvesCronPreferringConfigThenStepThenColumn() {
    Pipeline pipeline = new Pipeline();
    pipeline.setExecutionConfig("{\"scheduleCron\":\"*/1 * * * *\"}");
    pipeline.setScheduleCron("0 */2 * * *");
    PipelineStep step = new PipelineStep();
    step.setPipeletId("plet-schedule-source");
    step.setExecutionConfig("{\"cron\":\"*/15 * * * *\"}");

    // UI execution_config wins over a stale schedule_cron column.
    assertThat(PipelineSchedulePoller.resolveCron(pipeline, List.of(step)))
        .isEqualTo("*/1 * * * *");

    pipeline.setExecutionConfig("{}");
    assertThat(PipelineSchedulePoller.resolveCron(pipeline, List.of(step)))
        .isEqualTo("*/15 * * * *");

    step.setExecutionConfig("{}");
    assertThat(PipelineSchedulePoller.resolveCron(pipeline, List.of(step))).isEqualTo("0 */2 * * *");
  }
}
