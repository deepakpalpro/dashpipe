package com.dashflow.api.webhook;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dashflow.api.config.WebhookRateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebhookRateLimiterTest {

  private WebhookRateLimiter limiter;

  @BeforeEach
  void setUp() {
    WebhookRateLimitProperties props = new WebhookRateLimitProperties();
    props.setEnabled(true);
    props.setRequestsPerWindow(3);
    props.setWindowSeconds(60);
    props.setRetryAfterSeconds(30);
    limiter = new WebhookRateLimiter(props);
  }

  @Test
  void allowsUpToLimit_thenThrows429() {
    assertThatCode(() -> limiter.checkOrThrow("T001")).doesNotThrowAnyException();
    assertThatCode(() -> limiter.checkOrThrow("T001")).doesNotThrowAnyException();
    assertThatCode(() -> limiter.checkOrThrow("T001")).doesNotThrowAnyException();
    assertThatThrownBy(() -> limiter.checkOrThrow("T001"))
        .isInstanceOf(WebhookRateLimitExceededException.class)
        .extracting(ex -> ((WebhookRateLimitExceededException) ex).getRetryAfterSeconds())
        .isEqualTo(30);
  }

  @Test
  void separateTenants_haveSeparateBuckets() {
    assertThatCode(() -> limiter.checkOrThrow("A")).doesNotThrowAnyException();
    assertThatCode(() -> limiter.checkOrThrow("A")).doesNotThrowAnyException();
    assertThatCode(() -> limiter.checkOrThrow("A")).doesNotThrowAnyException();
    assertThatCode(() -> limiter.checkOrThrow("B")).doesNotThrowAnyException();
    assertThatThrownBy(() -> limiter.checkOrThrow("A"))
        .isInstanceOf(WebhookRateLimitExceededException.class);
  }
}
