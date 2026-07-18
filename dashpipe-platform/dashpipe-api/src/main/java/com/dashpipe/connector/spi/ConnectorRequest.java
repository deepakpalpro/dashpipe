package com.dashpipe.connector.spi;

import java.util.Map;

/** Request payload for connector read/write (architecture §9.2). */
public record ConnectorRequest(String recordId, Map<String, Object> headers, byte[] payload) {

  public ConnectorRequest {
    headers = headers == null ? Map.of() : Map.copyOf(headers);
    payload = payload == null ? new byte[0] : payload;
  }
}
