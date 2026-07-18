package com.dashpipe.api.billing;

/** Thrown when a pipeline run is blocked by hard quota or zero credit (maps to HTTP 402). */
public class RunBlockedException extends RuntimeException {

  private final QuotaDecision decision;

  public RunBlockedException(QuotaDecision decision) {
    super(decision == null ? "run blocked" : decision.message());
    this.decision = decision;
  }

  public QuotaDecision getDecision() {
    return decision;
  }
}
