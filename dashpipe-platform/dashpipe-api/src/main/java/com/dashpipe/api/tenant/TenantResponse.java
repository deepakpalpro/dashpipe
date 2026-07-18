package com.dashpipe.api.tenant;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantResponse(
    String id,
    String name,
    String slug,
    TenantStatus status,
    BigDecimal creditBalance,
    Instant createdAt,
    Instant updatedAt) {

  static TenantResponse from(Tenant tenant) {
    return new TenantResponse(
        tenant.getId(),
        tenant.getName(),
        tenant.getSlug(),
        tenant.getStatus(),
        tenant.getCreditBalance(),
        tenant.getCreatedAt(),
        tenant.getUpdatedAt());
  }
}
