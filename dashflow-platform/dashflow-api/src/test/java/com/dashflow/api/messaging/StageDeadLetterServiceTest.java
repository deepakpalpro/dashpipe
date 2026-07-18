package com.dashflow.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.dashflow.spi.messaging.MessageBroker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StageDeadLetterServiceTest {

  @Mock private MessageBroker messageBroker;

  private StageDeadLetterService service;

  @BeforeEach
  void setUp() {
    service = new StageDeadLetterService(messageBroker, new ObjectMapper());
  }

  @Test
  void retriesUntilExhaustedThenDlq() {
    RetryPolicy policy = new RetryPolicy(2, 2.0, 1L, 10L);

    boolean first = service.handleFailure("T1", "P1", 1, "poison", 0, policy, "boom");
    assertThat(first).isFalse();
    verify(messageBroker)
        .publish(eq(QueueNaming.stageInputQueue("T1", "P1", 1)), any(byte[].class), anyMap());

    boolean second = service.handleFailure("T1", "P1", 1, "poison", 1, policy, "boom");
    assertThat(second).isTrue();
    verify(messageBroker)
        .deadLetter(eq(QueueNaming.stageDlq("T1", "P1", 1)), any(byte[].class), anyMap());
  }
}
