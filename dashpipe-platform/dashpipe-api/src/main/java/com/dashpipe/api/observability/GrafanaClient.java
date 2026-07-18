package com.dashpipe.api.observability;

/** Client for Grafana org/dashboard provisioning (architecture §7.2). */
public interface GrafanaClient {

  /** Creates (or returns) a Grafana organization for the tenant. */
  GrafanaOrg createOrg(String tenantId, String orgName);

  /** Upserts a dashboard into the given org. Returns dashboard uid. */
  String upsertDashboard(long orgId, String dashboardJson);
}
