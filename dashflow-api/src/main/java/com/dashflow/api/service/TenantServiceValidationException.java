package com.dashflow.api.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TenantServiceValidationException extends RuntimeException {

  public TenantServiceValidationException(String message) {
    super(message);
  }
}
