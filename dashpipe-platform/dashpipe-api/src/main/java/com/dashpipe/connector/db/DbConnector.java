package com.dashpipe.connector.db;

import com.dashpipe.connector.spi.ConnectionTestResult;
import com.dashpipe.connector.spi.Connector;
import com.dashpipe.connector.spi.ConnectorConfig;
import com.dashpipe.connector.spi.ConnectorContext;
import com.dashpipe.connector.spi.ConnectorRequest;
import com.dashpipe.connector.spi.ConnectorResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Database connector (JDBC). Pipelets typically open JDBC from CONNECTOR_CONFIG JSON. */
@Component
public class DbConnector implements Connector {

  public static final String TYPE = "db";
  public static final String SPI_VERSION = "1.0";
  public static final String PROP_JDBC_URL = "jdbcUrl";
  public static final String PROP_USERNAME = "username";
  public static final String PROP_PASSWORD = "password";

  private ConnectorConfig config;

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getSpiVersion() {
    return SPI_VERSION;
  }

  @Override
  public void configure(ConnectorContext context, ConnectorConfig config) {
    this.config = config;
  }

  @Override
  public ConnectionTestResult testConnection() {
    if (config == null) {
      return ConnectionTestResult.failed("Connector is not configured");
    }
    String jdbcUrl = stringProp(PROP_JDBC_URL, null);
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      return ConnectionTestResult.failed("Missing required property: jdbcUrl");
    }
    String user = stringProp(PROP_USERNAME, "");
    String password = stringProp(PROP_PASSWORD, "");
    long started = System.nanoTime();
    try (Connection ignored = DriverManager.getConnection(jdbcUrl, user, password)) {
      long latencyMs = (System.nanoTime() - started) / 1_000_000L;
      return ConnectionTestResult.ok(latencyMs, "JDBC connection successful");
    } catch (Exception ex) {
      long latencyMs = (System.nanoTime() - started) / 1_000_000L;
      return new ConnectionTestResult(
          false, latencyMs, "JDBC connection failed: " + ex.getClass().getSimpleName());
    }
  }

  @Override
  public ConnectorResponse read(ConnectorRequest request) {
    return ConnectorResponse.failure(
        "DbConnector.read is not used; pipelets execute SQL from CONNECTOR_CONFIG");
  }

  @Override
  public ConnectorResponse write(ConnectorRequest request) {
    return ConnectorResponse.failure(
        "DbConnector.write is not used; pipelets execute SQL from CONNECTOR_CONFIG");
  }

  @Override
  public void close() {
    this.config = null;
  }

  private String stringProp(String key, String defaultValue) {
    if (config == null) {
      return defaultValue;
    }
    Map<String, String> secrets = config.secrets();
    if (secrets != null && secrets.get(key) != null) {
      return secrets.get(key);
    }
    Map<String, Object> props = config.properties();
    if (props != null && props.get(key) != null) {
      return String.valueOf(props.get(key));
    }
    return defaultValue;
  }
}
