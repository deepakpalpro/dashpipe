package com.dashpipe.api.messaging;

import com.dashpipe.spi.messaging.LogicalDestinations;
import com.dashpipe.spi.messaging.MessageBroker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Routes poison stage messages to the per-stage DLQ after retries are exhausted (architecture §8.2).
 */
@Service
public class StageDeadLetterService {

  public static final String HEADER_FAILURE_COUNT = "x-pipeline-failure-count";
  public static final String HEADER_ERROR = "x-pipeline-error";

  private final MessageBroker messageBroker;
  private final ObjectMapper objectMapper;

  public StageDeadLetterService(MessageBroker messageBroker, ObjectMapper objectMapper) {
    this.messageBroker = messageBroker;
    this.objectMapper = objectMapper;
  }

  /**
   * Handles a failed stage delivery: republish to the input queue while retries remain, otherwise
   * publish to the stage DLQ.
   *
   * @return {@code true} if the message was dead-lettered
   */
  public boolean handleFailure(
      String tenantId,
      String pipelineId,
      int stageOrder,
      Object payload,
      int previousFailureCount,
      RetryPolicy policy,
      String errorSummary) {
    int failureCount = previousFailureCount + 1;
    Map<String, Object> headers = new LinkedHashMap<>();
    headers.put(HEADER_FAILURE_COUNT, failureCount);
    headers.put(HEADER_ERROR, errorSummary);
    byte[] body = toBytes(payload);

    if (policy.shouldRetry(failureCount)) {
      messageBroker.publish(
          LogicalDestinations.stageInputQueue(tenantId, pipelineId, stageOrder), body, headers);
      return false;
    }

    messageBroker.deadLetter(
        LogicalDestinations.stageDlq(tenantId, pipelineId, stageOrder), body, headers);
    return true;
  }

  private byte[] toBytes(Object payload) {
    if (payload == null) {
      return new byte[0];
    }
    if (payload instanceof byte[] bytes) {
      return bytes;
    }
    if (payload instanceof String s) {
      return s.getBytes(StandardCharsets.UTF_8);
    }
    try {
      return objectMapper.writeValueAsBytes(payload);
    } catch (JsonProcessingException e) {
      return String.valueOf(payload).getBytes(StandardCharsets.UTF_8);
    }
  }
}
