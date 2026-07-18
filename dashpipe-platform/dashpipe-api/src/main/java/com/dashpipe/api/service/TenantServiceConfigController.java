package com.dashpipe.api.service;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
public class TenantServiceConfigController {

  private final TenantServiceConfigService tenantServiceConfigService;

  public TenantServiceConfigController(TenantServiceConfigService tenantServiceConfigService) {
    this.tenantServiceConfigService = tenantServiceConfigService;
  }

  @PostMapping
  public ResponseEntity<TenantServiceResponse> create(
      @Valid @RequestBody CreateTenantServiceRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tenantServiceConfigService.create(request));
  }

  @GetMapping("/{id}")
  public TenantServiceResponse get(@PathVariable String id) {
    return tenantServiceConfigService.get(id);
  }

  @GetMapping
  public List<TenantServiceResponse> list() {
    return tenantServiceConfigService.list();
  }

  @PutMapping("/{id}")
  public TenantServiceResponse update(
      @PathVariable String id, @Valid @RequestBody UpdateTenantServiceRequest request) {
    return tenantServiceConfigService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    tenantServiceConfigService.delete(id);
  }
}
