package com.dashflow.api.observability;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Completeness calculation (architecture §7.4):
 *
 * <pre>
 * completeness_pct = (total_records_out / total_records_in) × 100
 * </pre>
 *
 * <p>When {@code recordsIn == 0}, ratio and percent are {@code 0} (documented Wave 4 policy).
 */
public final class CompletenessCalculator {

  private CompletenessCalculator() {}

  public record Result(long recordsIn, long recordsOut, double ratio, BigDecimal percent) {}

  public static Result calculate(long recordsIn, long recordsOut) {
    long in = Math.max(0L, recordsIn);
    long out = Math.max(0L, recordsOut);
    if (in == 0L) {
      return new Result(in, out, 0.0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }
    double ratio = (double) out / (double) in;
    BigDecimal percent =
        BigDecimal.valueOf(ratio * 100.0).setScale(2, RoundingMode.HALF_UP);
    return new Result(in, out, ratio, percent);
  }
}
