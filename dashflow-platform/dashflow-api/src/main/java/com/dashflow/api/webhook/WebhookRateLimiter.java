package com.dashflow.api.webhook;

import com.dashflow.api.config.WebhookRateLimitProperties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Fixed-window per-tenant rate limiter for public webhook ingress (W3-US04). */
@Component
public class WebhookRateLimiter {

  private final WebhookRateLimitProperties properties;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  public WebhookRateLimiter(WebhookRateLimitProperties properties) {
    this.properties = properties;
  }

  /**
   * @throws WebhookRateLimitExceededException when the tenant exceeds the configured window
   */
  public void checkOrThrow(String tenantId) {
    if (!properties.isEnabled()) {
      return;
    }
    if (tenantId == null || tenantId.isBlank()) {
      return;
    }
    int limit = Math.max(1, properties.getRequestsPerWindow());
    int windowSeconds = Math.max(1, properties.getWindowSeconds());
    long nowSec = System.currentTimeMillis() / 1000L;
    long windowStart = nowSec - (nowSec % windowSeconds);

    Window window =
        windows.compute(
            tenantId,
            (key, existing) -> {
              if (existing == null || existing.windowStart != windowStart) {
                return new Window(windowStart, new AtomicInteger(0));
              }
              return existing;
            });

    int count = window.count.incrementAndGet();
    if (count > limit) {
      throw new WebhookRateLimitExceededException(
          "Webhook rate limit exceeded for tenant " + tenantId,
          Math.max(1, properties.getRetryAfterSeconds()));
    }
  }

  /** Test helper. */
  public void clear() {
    windows.clear();
  }

  private record Window(long windowStart, AtomicInteger count) {}
}
