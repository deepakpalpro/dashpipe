package com.dashpipe.api.billing;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Pure quota / credit decision (architecture §6.2). Soft allows execution; hard / zero credit block
 * (HTTP 402 wired in W5-US06).
 */
@Component
public class QuotaEvaluator {

  /**
   * Evaluate credit and per-dimension usage against config.
   *
   * <p>Priority: {@code NO_CREDIT} → {@code HARD_BLOCK} → {@code SOFT_WARN} → {@code ALLOW}.
   */
  public QuotaDecision evaluate(
      BigDecimal creditBalance, QuotaConfig config, Map<String, BigDecimal> usageByDimension) {
    BigDecimal credit = creditBalance == null ? BigDecimal.ZERO : creditBalance;
    if (credit.compareTo(BigDecimal.ZERO) <= 0) {
      return QuotaDecision.noCredit(credit);
    }

    QuotaConfig cfg = config == null ? QuotaConfig.empty() : config;
    Map<String, BigDecimal> usage =
        usageByDimension == null ? Map.of() : usageByDimension;

    QuotaDecision soft = null;
    for (Map.Entry<String, QuotaConfig.DimensionLimit> entry : cfg.dimensions().entrySet()) {
      String dimension = entry.getKey();
      QuotaConfig.DimensionLimit limit = entry.getValue();
      BigDecimal current = usage.getOrDefault(dimension, BigDecimal.ZERO);

      if (limit.hard() != null && current.compareTo(limit.hard()) >= 0) {
        return QuotaDecision.hardBlock(credit, dimension, limit.soft(), limit.hard(), current);
      }
      if (limit.soft() != null
          && current.compareTo(limit.soft()) >= 0
          && soft == null) {
        soft =
            QuotaDecision.softWarn(credit, dimension, limit.soft(), limit.hard(), current);
      }
    }
    return soft != null ? soft : QuotaDecision.allow(credit);
  }
}
