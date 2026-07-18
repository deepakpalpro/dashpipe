package com.dashflow.connector.rest;

import com.dashflow.connector.spi.ConnectionTestResult;
import com.dashflow.connector.spi.Connector;
import com.dashflow.connector.spi.ConnectorConfig;
import com.dashflow.connector.spi.ConnectorContext;
import com.dashflow.connector.spi.ConnectorRequest;
import com.dashflow.connector.spi.ConnectorResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Built-in REST connector (architecture §9.5). W1-US06 probes {@code baseUrl + pingPath} (default
 * {@code /external/ping}) via HTTP GET.
 */
@Component
public class RestConnector implements Connector {

  public static final String TYPE = "rest";
  public static final String SPI_VERSION = "1.0";
  public static final String DEFAULT_PING_PATH = "/external/ping";
  public static final String PROP_BASE_URL = "baseUrl";
  public static final String PROP_PING_PATH = "pingPath";
  public static final String PROP_TIMEOUT_MS = "timeoutMs";

  private final HttpClient httpClient;

  private ConnectorContext context;
  private ConnectorConfig config;

  public RestConnector() {
    this(
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build());
  }

  RestConnector(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

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
    this.context = context;
    this.config = config;
  }

  @Override
  public ConnectionTestResult testConnection() {
    if (config == null || config.properties().isEmpty()) {
      return ConnectionTestResult.failed("Connector is not configured");
    }
    Object baseUrl = config.properties().get(PROP_BASE_URL);
    if (baseUrl == null || baseUrl.toString().isBlank()) {
      return ConnectionTestResult.failed("Missing required property: baseUrl");
    }

    String pingPath = propertyAsString(PROP_PING_PATH, DEFAULT_PING_PATH);
    long timeoutMs = propertyAsLong(PROP_TIMEOUT_MS, 5_000L);
    URI target = joinUrl(baseUrl.toString(), pingPath);

    long started = System.nanoTime();
    try {
      HttpRequest request =
          HttpRequest.newBuilder(target)
              .timeout(Duration.ofMillis(timeoutMs))
              .header("Accept", "application/json")
              .GET()
              .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      long latencyMs = (System.nanoTime() - started) / 1_000_000L;
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        return ConnectionTestResult.ok(latencyMs, "Connection successful");
      }
      return new ConnectionTestResult(
          false, latencyMs, "HTTP " + response.statusCode() + " from " + target);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return ConnectionTestResult.failed("Interrupted while testing " + target);
    } catch (Exception ex) {
      long latencyMs = (System.nanoTime() - started) / 1_000_000L;
      return new ConnectionTestResult(
          false, latencyMs, "Connection failed: " + ex.getClass().getSimpleName());
    }
  }

  @Override
  public ConnectorResponse read(ConnectorRequest request) {
    return ConnectorResponse.failure("read not implemented");
  }

  @Override
  public ConnectorResponse write(ConnectorRequest request) {
    return ConnectorResponse.failure("write not implemented");
  }

  @Override
  public void close() {
    this.context = null;
    this.config = null;
  }

  static URI joinUrl(String baseUrl, String path) {
    String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    String suffix = path.startsWith("/") ? path : "/" + path;
    return URI.create(base + suffix);
  }

  private String propertyAsString(String key, String defaultValue) {
    Object value = config.properties().get(key);
    if (value == null || value.toString().isBlank()) {
      return defaultValue;
    }
    return value.toString();
  }

  private long propertyAsLong(String key, long defaultValue) {
    Object value = config.properties().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }
}
