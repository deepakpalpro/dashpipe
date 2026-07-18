package com.dashpipe.api.usage;

import java.math.BigDecimal;

/** Projection for SUM(quantity) grouped by tenant + dimension. */
public interface UsageDimensionSum {

  String getTenantId();

  String getDimension();

  BigDecimal getTotalQuantity();
}
