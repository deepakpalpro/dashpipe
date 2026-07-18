package com.dashpipe.api.connector;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TenantConnectorConflictException extends RuntimeException {

  public TenantConnectorConflictException(String message) {
    super(message);
  }
}
