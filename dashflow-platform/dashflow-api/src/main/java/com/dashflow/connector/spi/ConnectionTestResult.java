package com.dashflow.connector.spi;

/** Result of {@link Connector#testConnection()} (architecture §9.2). */
public record ConnectionTestResult(boolean success, long latencyMs, String message) {

  public static ConnectionTestResult ok(long latencyMs, String message) {
    return new ConnectionTestResult(true, latencyMs, message);
  }

  public static ConnectionTestResult failed(String message) {
    return new ConnectionTestResult(false, 0, message);
  }
}
