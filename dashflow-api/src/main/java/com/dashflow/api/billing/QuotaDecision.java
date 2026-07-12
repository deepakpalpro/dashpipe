package com.dashflow.api.billing;

import java.math.BigDecimal;

/**
 * Result of {@link QuotaEvaluator}. {@link #allowed()} is true for {@link QuotaDecisionCode#ALLOW}
 * and {@link QuotaDecisionCode#SOFT_WARN} only.
 */
public record QuotaDecision(
    QuotaDecisionCode code,
    String message,
    String breachedDimension,
    BigDecimal creditBalance,
    BigDecimal softLimit,
    BigDecimal hardLimit,
    BigDecimal currentUsage) {

  public boolean allowed() {
    return code == QuotaDecisionCode.ALLOW || code == QuotaDecisionCode.SOFT_WARN;
  }

  public boolean blocksRun() {
    return code == QuotaDecisionCode.HARD_BLOCK || code == QuotaDecisionCode.NO_CREDIT;
  }

  public static QuotaDecision allow(BigDecimal creditBalance) {
    return new QuotaDecision(
        QuotaDecisionCode.ALLOW, "within quota", null, creditBalance, null, null, null);
  }

  public static QuotaDecision softWarn(
      BigDecimal creditBalance,
      String dimension,
      BigDecimal soft,
      BigDecimal hard,
      BigDecimal usage) {
    return new QuotaDecision(
        QuotaDecisionCode.SOFT_WARN,
        "soft limit reached for " + dimension,
        dimension,
        creditBalance,
        soft,
        hard,
        usage);
  }

  public static QuotaDecision hardBlock(
      BigDecimal creditBalance,
      String dimension,
      BigDecimal soft,
      BigDecimal hard,
      BigDecimal usage) {
    return new QuotaDecision(
        QuotaDecisionCode.HARD_BLOCK,
        "hard limit reached for " + dimension,
        dimension,
        creditBalance,
        soft,
        hard,
        usage);
  }

  public static QuotaDecision noCredit(BigDecimal creditBalance) {
    return new QuotaDecision(
        QuotaDecisionCode.NO_CREDIT,
        "credit balance exhausted",
        null,
        creditBalance,
        null,
        null,
        null);
  }
}
