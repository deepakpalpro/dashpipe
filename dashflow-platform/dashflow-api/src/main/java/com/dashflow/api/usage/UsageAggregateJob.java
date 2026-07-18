package com.dashflow.api.usage;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cron: top of each hour UTC — roll previous hour into usage_aggregates (§6.2). */
@Component
public class UsageAggregateJob {

  private final UsageAggregateService aggregateService;

  public UsageAggregateJob(UsageAggregateService aggregateService) {
    this.aggregateService = aggregateService;
  }

  @Scheduled(cron = "${pipeline.usage.aggregate.cron:0 0 * * * *}", zone = "UTC")
  public void runHourly() {
    aggregateService.aggregatePreviousHour();
  }
}
