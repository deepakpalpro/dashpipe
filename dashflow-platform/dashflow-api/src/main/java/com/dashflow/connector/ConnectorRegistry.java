package com.dashflow.connector;

import com.dashflow.connector.spi.Connector;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-process connector plugin registry. Wave 1 uses Spring bean injection; PF4J directory scan is
 * deferred (architecture §9.4).
 */
@Component
public class ConnectorRegistry {

  private static final Logger log = LoggerFactory.getLogger(ConnectorRegistry.class);

  private final Map<String, Connector> byType = new LinkedHashMap<>();

  public ConnectorRegistry(List<Connector> connectors) {
    for (Connector connector : connectors) {
      String type = connector.getType();
      if (byType.containsKey(type)) {
        throw new IllegalStateException("Duplicate connector type registered: " + type);
      }
      byType.put(type, connector);
      log.info(
          "Registered connector plugin type={} spiVersion={} class={}",
          type,
          connector.getSpiVersion(),
          connector.getClass().getName());
    }
  }

  public Optional<Connector> findByType(String type) {
    return Optional.ofNullable(byType.get(type));
  }

  public Collection<Connector> list() {
    return List.copyOf(byType.values());
  }

  public boolean hasType(String type) {
    return byType.containsKey(type);
  }
}
