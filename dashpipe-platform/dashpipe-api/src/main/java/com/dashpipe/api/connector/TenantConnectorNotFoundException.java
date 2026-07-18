package com.dashpipe.api.connector;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TenantConnectorNotFoundException extends RuntimeException {

  public TenantConnectorNotFoundException(String id) {
    super("Connector not found: " + id);
  }
}
