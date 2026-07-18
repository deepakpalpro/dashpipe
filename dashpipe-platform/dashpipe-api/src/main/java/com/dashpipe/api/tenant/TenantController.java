package com.dashpipe.api.tenant;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1/tenants")
public class TenantController {

  private final TenantService tenantService;

  public TenantController(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @PostMapping
  public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(request));
  }

  @GetMapping("/{id}")
  public TenantResponse get(@PathVariable String id) {
    return tenantService.get(id);
  }

  @GetMapping
  public List<TenantResponse> list() {
    return tenantService.list();
  }

  @PutMapping("/{id}")
  public TenantResponse update(
      @PathVariable String id, @Valid @RequestBody UpdateTenantRequest request) {
    return tenantService.update(id, request);
  }

  /**
   * Dev helper for W1-US01: proves {@link TenantContext} was populated from {@code X-Tenant-Id}.
   * Not a business API — remove or gate when real auth lands.
   */
  @GetMapping("/_context")
  public Map<String, String> currentContext() {
    String tenantId = TenantContext.getTenantId();
    return Map.of("tenantId", tenantId == null ? "" : tenantId);
  }
}
