package com.dashpipe.api.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** HTTP 402 body when a pipeline run is blocked by quota / credit (§6.2). */
public record RunBlockedResponse(
    @JsonProperty("error") String error,
    @JsonProperty("code") String code,
    @JsonProperty("message") String message,
    @JsonProperty("credit_balance") BigDecimal creditBalance,
    @JsonProperty("breached_dimension") String breachedDimension,
    @JsonProperty("soft_limit") BigDecimal softLimit,
    @JsonProperty("hard_limit") BigDecimal hardLimit,
    @JsonProperty("current_usage") BigDecimal currentUsage) {

  public static RunBlockedResponse from(QuotaDecision decision) {
    return new RunBlockedResponse(
        "payment_required",
        decision.code().name(),
        decision.message(),
        decision.creditBalance(),
        decision.breachedDimension(),
        decision.softLimit(),
        decision.hardLimit(),
        decision.currentUsage());
  }
}
