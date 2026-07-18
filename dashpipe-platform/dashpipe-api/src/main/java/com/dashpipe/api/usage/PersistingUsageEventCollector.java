package com.dashpipe.api.usage;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Durable {@link UsageEventCollector} (W5-US01). Primary bean so {@link UsageEventEmitter} persists
 * to MySQL. Unit tests may still construct {@link StubUsageEventCollector} directly.
 */
@Component
@Primary
public class PersistingUsageEventCollector implements UsageEventCollector {

  private final UsageEventService usageEventService;

  public PersistingUsageEventCollector(UsageEventService usageEventService) {
    this.usageEventService = usageEventService;
  }

  @Override
  public void collect(UsageEvent event) {
    if (event == null) {
      return;
    }
    usageEventService.persist(event);
  }
}
