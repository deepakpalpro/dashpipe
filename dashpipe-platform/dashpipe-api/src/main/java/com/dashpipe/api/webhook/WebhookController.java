package com.dashpipe.api.webhook;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

  private final WebhookIngressService webhookIngressService;
  private final WebhookSignatureVerifier signatureVerifier;
  private final WebhookRateLimiter rateLimiter;

  public WebhookController(
      WebhookIngressService webhookIngressService,
      WebhookSignatureVerifier signatureVerifier,
      WebhookRateLimiter rateLimiter) {
    this.webhookIngressService = webhookIngressService;
    this.signatureVerifier = signatureVerifier;
    this.rateLimiter = rateLimiter;
  }

  @PostMapping("/{tenantId}/{connectorId}")
  public ResponseEntity<WebhookAcceptResponse> accept(
      @PathVariable String tenantId,
      @PathVariable String connectorId,
      @RequestBody(required = false) byte[] rawBody,
      HttpServletRequest request) {
    rateLimiter.checkOrThrow(tenantId);
    String headerName = signatureVerifier.resolveSignatureHeader(tenantId);
    String signature = request.getHeader(headerName);
    String webhookId = request.getHeader(WebhookIdempotencyService.WEBHOOK_ID_HEADER);
    WebhookAcceptResponse response =
        webhookIngressService.accept(tenantId, connectorId, rawBody, signature, webhookId);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
  }
}
