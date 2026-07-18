package com.dashpipe.api.tenant;

/** Shared Hibernate filter names for tenant isolation (W1-US02). */
public final class TenantFilters {

  public static final String NAME = "tenantFilter";
  public static final String PARAM_TENANT_ID = "tenantId";

  private TenantFilters() {}
}
