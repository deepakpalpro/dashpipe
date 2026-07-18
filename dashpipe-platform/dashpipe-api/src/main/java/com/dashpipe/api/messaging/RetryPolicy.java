package com.dashpipe.api.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pipeline retry policy (architecture §8.1 {@code retry_config}).
 *
 * <p>{@code maxRetries} is the number of failed attempts before the message is dead-lettered.
 */
public record RetryPolicy(
    int maxRetries, double backoffMultiplier, long initialDelayMs, long maxDelayMs) {

  public static final RetryPolicy DEFAULTS = new RetryPolicy(3, 2.0, 1000L, 60_000L);

  public RetryPolicy {
    if (maxRetries < 1) {
      throw new IllegalArgumentException("maxRetries must be >= 1");
    }
    if (backoffMultiplier < 1.0) {
      throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
    }
    if (initialDelayMs < 0 || maxDelayMs < 0) {
      throw new IllegalArgumentException("delays must be >= 0");
    }
  }

  public static RetryPolicy defaults() {
    return DEFAULTS;
  }

  public static RetryPolicy fromJson(String json, ObjectMapper objectMapper) {
    if (json == null || json.isBlank()) {
      return defaults();
    }
    try {
      JsonNode node = objectMapper.readTree(json);
      int maxRetries = node.path("max_retries").asInt(DEFAULTS.maxRetries());
      double backoff = node.path("backoff_multiplier").asDouble(DEFAULTS.backoffMultiplier());
      long initial = node.path("initial_delay_ms").asLong(DEFAULTS.initialDelayMs());
      long maxDelay = node.path("max_delay_ms").asLong(DEFAULTS.maxDelayMs());
      return new RetryPolicy(maxRetries, backoff, initial, maxDelay);
    } catch (Exception ex) {
      return defaults();
    }
  }

  /**
   * @param failureCount failures observed so far (1 after the first failure)
   * @return true if another attempt should be made
   */
  public boolean shouldRetry(int failureCount) {
    return failureCount < maxRetries;
  }

  /** True when this failure should be dead-lettered (no further retries). */
  public boolean isExhausted(int failureCount) {
    return failureCount >= maxRetries;
  }

  public long delayMsForFailure(int failureCount) {
    if (failureCount < 1) {
      return initialDelayMs;
    }
    double raw = initialDelayMs * Math.pow(backoffMultiplier, failureCount - 1);
    return Math.min(maxDelayMs, (long) raw);
  }
}
