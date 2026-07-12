package com.dashflow.api.observability;

/**
 * Allowlisted {@code error_type} labels for {@code pipelet_errors_total} (architecture §7.1). Keep
 * this enum small to bound Prometheus cardinality.
 */
public enum PipeletErrorType {
  PROCESSING,
  TIMEOUT,
  VALIDATION,
  IMAGE_PULL,
  UNKNOWN;

  public String prometheusLabel() {
    return name().toLowerCase();
  }
}
