package com.dashpipe.api.connector;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TenantConnectorValidationException extends RuntimeException {

  public TenantConnectorValidationException(String message) {
    super(message);
  }
}
