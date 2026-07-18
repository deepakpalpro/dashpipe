package com.dashpipe.api.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class WebhookBrokerUnavailableException extends RuntimeException {

  public WebhookBrokerUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }

  public WebhookBrokerUnavailableException(String message) {
    super(message);
  }
}
