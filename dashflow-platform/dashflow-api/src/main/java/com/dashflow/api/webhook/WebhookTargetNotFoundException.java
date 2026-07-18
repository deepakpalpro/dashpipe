package com.dashflow.api.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WebhookTargetNotFoundException extends RuntimeException {

  public WebhookTargetNotFoundException(String tenantId, String connectorId) {
    super("Webhook target not found: tenant=" + tenantId + " connector=" + connectorId);
  }
}
