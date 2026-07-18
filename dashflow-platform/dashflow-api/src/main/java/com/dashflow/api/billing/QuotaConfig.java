package com.dashflow.api.billing;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/** Parsed {@code tenants.quota_config} JSON. */
public record QuotaConfig(Map<String, DimensionLimit> dimensions) {

  public QuotaConfig {
    dimensions = dimensions == null ? Map.of() : Map.copyOf(dimensions);
  }

  public static QuotaConfig empty() {
    return new QuotaConfig(Collections.emptyMap());
  }

  public record DimensionLimit(BigDecimal soft, BigDecimal hard) {}
}
