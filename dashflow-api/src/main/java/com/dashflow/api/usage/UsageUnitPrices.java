package com.dashflow.api.usage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Stub unit prices from architecture §6.2 (Wave 5). Shared by aggregate job and later billing APIs.
 */
public final class UsageUnitPrices {

  private static final Map<String, BigDecimal> PRICE_PER_UNIT =
      Map.of(
          UsageEvent.VCPU_SECONDS, bd("0.005"),
          UsageEvent.RECORDS_PROCESSED, bd("0.00001"),
          UsageEvent.CONNECTOR_API_CALLS, bd("0.0005"),
          UsageEvent.PIPELINE_RUNS, bd("0.01"),
          UsageEvent.WEBHOOK_EVENTS, bd("0.000005"),
          // $0.01 / GB → per byte
          UsageEvent.BYTES_IN, bd("0.00000000001"));

  private UsageUnitPrices() {}

  public static BigDecimal costFor(String dimension, BigDecimal quantity) {
    if (quantity == null) {
      return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }
    BigDecimal unit = PRICE_PER_UNIT.getOrDefault(dimension, BigDecimal.ZERO);
    return quantity.multiply(unit).setScale(4, RoundingMode.HALF_UP);
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
