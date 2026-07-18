package com.dashflow.api.observability;

/** Result of provisioning Grafana for a tenant. */
public record GrafanaProvisionResult(
    String tenantId, long orgId, String orgName, String dashboardUid) {}
