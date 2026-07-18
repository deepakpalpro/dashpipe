package com.dashflow.api.observability;

import com.dashflow.api.tenant.Tenant;
import com.dashflow.api.tenant.TenantNotFoundException;
import com.dashflow.api.tenant.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Optional admin hook to provision a Grafana <em>org</em> for a tenant on the shared instance (W4-US06 stub). */
@RestController
@RequestMapping("/api/v1/tenants")
public class GrafanaProvisionController {

  private final TenantRepository tenantRepository;
  private final GrafanaProvisioner grafanaProvisioner;

  public GrafanaProvisionController(
      TenantRepository tenantRepository, GrafanaProvisioner grafanaProvisioner) {
    this.tenantRepository = tenantRepository;
    this.grafanaProvisioner = grafanaProvisioner;
  }

  @PostMapping("/{id}/grafana")
  @ResponseStatus(HttpStatus.CREATED)
  public GrafanaProvisionResult provision(@PathVariable String id) {
    Tenant tenant =
        tenantRepository.findById(id).orElseThrow(() -> new TenantNotFoundException(id));
    return grafanaProvisioner.provisionTenant(tenant.getId(), tenant.getName());
  }
}
