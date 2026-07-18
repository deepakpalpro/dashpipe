package com.dashpipe.api.observability;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Provisions a Grafana <em>organization</em> + baseline dashboard for a tenant on the shared Grafana
 * instance (architecture §7.2). Does not deploy a Grafana server per tenant. Uses {@link
 * GrafanaClient} (stub in Wave 4).
 */
@Component
public class GrafanaProvisioner {

  public static final String TEMPLATE_PATH = "grafana/tenant-pipeline-overview.json";

  private final GrafanaClient grafanaClient;

  public GrafanaProvisioner(GrafanaClient grafanaClient) {
    this.grafanaClient = grafanaClient;
  }

  public GrafanaProvisionResult provisionTenant(String tenantId, String tenantName) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("tenantId required");
    }
    String orgName = "tenant-" + tenantId;
    if (tenantName != null && !tenantName.isBlank()) {
      orgName = tenantName + " (" + tenantId + ")";
    }
    GrafanaOrg org = grafanaClient.createOrg(tenantId, orgName);
    String dashboardJson = loadTemplate().replace("${tenant_id}", tenantId);
    String uid = grafanaClient.upsertDashboard(org.orgId(), dashboardJson);
    return new GrafanaProvisionResult(tenantId, org.orgId(), org.name(), uid);
  }

  private static String loadTemplate() {
    try (InputStream in = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Missing Grafana template: " + TEMPLATE_PATH, ex);
    }
  }
}
