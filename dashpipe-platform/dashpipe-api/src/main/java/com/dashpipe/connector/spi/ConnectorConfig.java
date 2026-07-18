package com.dashpipe.connector.spi;

import java.util.Map;

/** Tenant connector configuration (architecture §9.2). */
public record ConnectorConfig(Map<String, Object> properties, Map<String, String> secrets) {

  public ConnectorConfig {
    properties = properties == null ? Map.of() : Map.copyOf(properties);
    secrets = secrets == null ? Map.of() : Map.copyOf(secrets);
  }
}
