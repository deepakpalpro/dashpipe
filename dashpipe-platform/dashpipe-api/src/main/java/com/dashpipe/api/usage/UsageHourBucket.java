package com.dashpipe.api.usage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** UTC hour truncation helpers for usage rollups. */
public final class UsageHourBucket {

  private UsageHourBucket() {}

  /** Truncate to the start of the UTC hour containing {@code instant}. */
  public static Instant truncateToHour(Instant instant) {
    return instant.truncatedTo(ChronoUnit.HOURS);
  }

  /** Start of the previous complete UTC hour relative to {@code now}. */
  public static Instant previousHourStart(Instant now) {
    return truncateToHour(now).minus(1, ChronoUnit.HOURS);
  }

  public static Instant hourEnd(Instant periodStart) {
    return periodStart.plus(1, ChronoUnit.HOURS);
  }
}
