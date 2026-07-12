package com.dashflow.api.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TenantConflictException extends RuntimeException {

  public TenantConflictException(String message) {
    super(message);
  }
}
