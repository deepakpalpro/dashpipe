package com.dashflow.api.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TenantContextRequiredException extends RuntimeException {

  public TenantContextRequiredException() {
    super("X-Tenant-Id header is required for tenant-owned resources");
  }
}
