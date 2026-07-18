package com.dashpipe.api.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TenantNotFoundException extends RuntimeException {

  public TenantNotFoundException(String id) {
    super("Tenant not found: " + id);
  }
}
