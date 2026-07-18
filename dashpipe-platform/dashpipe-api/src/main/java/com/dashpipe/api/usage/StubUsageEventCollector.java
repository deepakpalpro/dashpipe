package com.dashpipe.api.usage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** In-memory collector for unit tests and local inspection (not a Spring bean). */
public class StubUsageEventCollector implements UsageEventCollector {

  private final List<UsageEvent> events = new CopyOnWriteArrayList<>();

  @Override
  public void collect(UsageEvent event) {
    if (event != null) {
      events.add(event);
    }
  }

  public List<UsageEvent> getEvents() {
    return Collections.unmodifiableList(new ArrayList<>(events));
  }

  public void clear() {
    events.clear();
  }
}
