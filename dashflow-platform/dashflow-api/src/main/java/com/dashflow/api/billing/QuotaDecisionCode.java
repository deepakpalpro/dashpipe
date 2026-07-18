package com.dashflow.api.billing;

/** Outcome of a quota / credit check (architecture §6.2). */
public enum QuotaDecisionCode {
  /** Under soft limits and credit &gt; 0. */
  ALLOW,
  /** At or above soft, below hard — run allowed; warn stub. */
  SOFT_WARN,
  /** At or above hard limit — block run (US06 → 402). */
  HARD_BLOCK,
  /** credit_balance ≤ 0 — block run (US06 → 402). */
  NO_CREDIT
}
