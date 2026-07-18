package com.dashpipe.api.connector;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/connectors")
public class TenantConnectorController {

  private final TenantConnectorService tenantConnectorService;
  private final WebhookUrlProvisionService webhookUrlProvisionService;

  public TenantConnectorController(
      TenantConnectorService tenantConnectorService,
      WebhookUrlProvisionService webhookUrlProvisionService) {
    this.tenantConnectorService = tenantConnectorService;
    this.webhookUrlProvisionService = webhookUrlProvisionService;
  }

  @PostMapping
  public ResponseEntity<TenantConnectorResponse> create(
      @Valid @RequestBody CreateConnectorRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tenantConnectorService.create(request));
  }

  @GetMapping("/{id}")
  public TenantConnectorResponse get(@PathVariable String id) {
    return tenantConnectorService.get(id);
  }

  @GetMapping
  public List<TenantConnectorResponse> list() {
    return tenantConnectorService.list();
  }

  @PutMapping("/{id}")
  public TenantConnectorResponse update(
      @PathVariable String id, @Valid @RequestBody UpdateConnectorRequest request) {
    return tenantConnectorService.update(id, request);
  }

  @PostMapping("/{id}/test")
  public ConnectionTestResponse test(@PathVariable String id) {
    return tenantConnectorService.test(id);
  }

  @PostMapping("/{id}/webhook-url")
  public WebhookUrlProvisionResponse provisionWebhookUrl(@PathVariable String id) {
    return webhookUrlProvisionService.provision(id);
  }
}
