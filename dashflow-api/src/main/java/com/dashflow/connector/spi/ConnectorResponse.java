package com.dashflow.connector.spi;

import java.util.Map;

/** Response from connector read/write (architecture §9.2). */
public record ConnectorResponse(
    boolean success,
    int statusCode,
    byte[] payload,
    Map<String, String> metadata,
    String errorMessage) {

  public ConnectorResponse {
    payload = payload == null ? new byte[0] : payload;
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  public static ConnectorResponse failure(String message) {
    return new ConnectorResponse(false, 0, new byte[0], Map.of(), message);
  }
}
