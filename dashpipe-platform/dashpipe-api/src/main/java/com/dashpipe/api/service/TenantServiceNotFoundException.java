package com.dashpipe.api.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TenantServiceNotFoundException extends RuntimeException {

  public TenantServiceNotFoundException(String id) {
    super("Tenant service not found: " + id);
  }
}
