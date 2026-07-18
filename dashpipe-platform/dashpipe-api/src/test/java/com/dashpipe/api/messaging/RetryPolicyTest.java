package com.dashpipe.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void exhaustsThenDlq() {
    RetryPolicy policy = new RetryPolicy(3, 2.0, 1L, 10L);
    int failures = 0;
    boolean sentToDlq = false;

    while (!sentToDlq) {
      failures++;
      if (policy.isExhausted(failures)) {
        sentToDlq = true;
      } else {
        assertThat(policy.shouldRetry(failures)).isTrue();
      }
    }

    assertThat(failures).isEqualTo(policy.maxRetries());
    assertThat(sentToDlq).isTrue();
    assertThat(policy.shouldRetry(3)).isFalse();
    assertThat(policy.shouldRetry(2)).isTrue();
  }

  @Test
  void fromJson_readsArchitectureShape() {
    RetryPolicy policy =
        RetryPolicy.fromJson(
            """
            {"max_retries":5,"backoff_multiplier":2.0,"initial_delay_ms":100,"max_delay_ms":1000}
            """,
            objectMapper);

    assertThat(policy.maxRetries()).isEqualTo(5);
    assertThat(policy.delayMsForFailure(1)).isEqualTo(100L);
    assertThat(policy.delayMsForFailure(2)).isEqualTo(200L);
    assertThat(policy.delayMsForFailure(4)).isEqualTo(800L);
    assertThat(policy.delayMsForFailure(5)).isEqualTo(1000L);
  }

  @Test
  void fromJson_blank_usesDefaults() {
    assertThat(RetryPolicy.fromJson(null, objectMapper)).isEqualTo(RetryPolicy.defaults());
    assertThat(RetryPolicy.fromJson(" ", objectMapper).maxRetries()).isEqualTo(3);
  }
}
