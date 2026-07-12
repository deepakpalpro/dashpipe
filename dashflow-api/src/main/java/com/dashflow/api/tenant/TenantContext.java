package com.dashflow.api.tenant;

/**
 * Request-scoped tenant id (W1-US01).
 *
 * <p>Populated by {@link TenantContextFilter} from stub header {@code X-Tenant-Id} (local/dev). Real
 * JWT claim resolution comes later.
 */
public final class TenantContext {

  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private TenantContext() {}

  public static void setTenantId(String tenantId) {
    CURRENT.set(tenantId);
  }

  public static String getTenantId() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
