package com.dashpipe.api.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class WebhookSignatureRejectedException extends RuntimeException {

  public WebhookSignatureRejectedException(String message) {
    super(message);
  }
}
