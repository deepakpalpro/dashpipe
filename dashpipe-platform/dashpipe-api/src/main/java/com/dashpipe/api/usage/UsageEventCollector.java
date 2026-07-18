package com.dashpipe.api.usage;

/** Sink for usage events (Wave 5 collector will replace / wrap the stub). */
public interface UsageEventCollector {

  void collect(UsageEvent event);
}
