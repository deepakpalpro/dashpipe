package com.dashpipe.api.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TenantValidationException extends RuntimeException {

  public TenantValidationException(String message) {
    super(message);
  }
}
