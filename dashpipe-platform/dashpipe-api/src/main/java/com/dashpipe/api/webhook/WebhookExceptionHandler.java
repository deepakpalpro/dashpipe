package com.dashpipe.api.webhook;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WebhookExceptionHandler {

  @ExceptionHandler(WebhookRateLimitExceededException.class)
  public ResponseEntity<Map<String, Object>> handleRateLimit(WebhookRateLimitExceededException ex) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .headers(headers)
        .body(
            Map.of(
                "error", "rate_limit_exceeded",
                "message", ex.getMessage(),
                "retry_after_seconds", ex.getRetryAfterSeconds()));
  }

  @ExceptionHandler(WebhookBrokerUnavailableException.class)
  public ResponseEntity<Map<String, Object>> handleBroker(WebhookBrokerUnavailableException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            Map.of(
                "error", "broker_unavailable",
                "message", ex.getMessage() == null ? "Webhook broker publish failed" : ex.getMessage()));
  }
}
