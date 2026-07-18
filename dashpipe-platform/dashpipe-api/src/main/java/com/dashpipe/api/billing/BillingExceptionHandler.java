package com.dashpipe.api.billing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BillingExceptionHandler {

  @ExceptionHandler(RunBlockedException.class)
  public ResponseEntity<RunBlockedResponse> handleRunBlocked(RunBlockedException ex) {
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(RunBlockedResponse.from(ex.getDecision()));
  }
}
