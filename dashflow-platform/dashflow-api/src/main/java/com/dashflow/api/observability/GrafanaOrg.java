package com.dashflow.api.observability;

/** Result of creating/looking up a Grafana organization. */
public record GrafanaOrg(long orgId, String name) {}
