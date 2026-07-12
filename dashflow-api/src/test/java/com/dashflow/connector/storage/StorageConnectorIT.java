package com.dashflow.connector.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.PipelineApiApplication;
import com.dashflow.connector.ConnectorRegistry;
import com.dashflow.connector.spi.ConnectionTestResult;
import com.dashflow.connector.spi.ConnectorConfig;
import com.dashflow.connector.spi.ConnectorContext;
import com.dashflow.connector.spi.ConnectorRequest;
import com.dashflow.connector.spi.ConnectorResponse;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * W1-US07: StorageConnector put/get round-trip against Compose LocalStack S3.
 */
@SpringBootTest(classes = PipelineApiApplication.class)
@ActiveProfiles("local")
class StorageConnectorIT {

  private static final String ENDPOINT =
      System.getenv().getOrDefault("LOCALSTACK_ENDPOINT", LocalStackS3ClientFactory.DEFAULT_ENDPOINT);

  @BeforeAll
  static void requireLocalStack() {
    assumeTrue(
        isLocalStackHealthy(ENDPOINT),
        "LocalStack is not reachable at "
            + ENDPOINT
            + " — run: docker compose up -d localstack && ./scripts/smoke-localstack.sh");
  }

  private static boolean isLocalStackHealthy(String endpoint) {
    try {
      URI health = URI.create(endpoint.replaceAll("/$", "") + "/_localstack/health");
      HttpResponse<String> response =
          HttpClient.newHttpClient()
              .send(
                  HttpRequest.newBuilder(health).GET().build(),
                  HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200;
    } catch (Exception ex) {
      try (Socket socket = new Socket("127.0.0.1", URI.create(endpoint).getPort())) {
        return socket.isConnected();
      } catch (Exception ignored) {
        return false;
      }
    }
  }

  @Autowired private ConnectorRegistry registry;

  @Test
  void registry_includesStorage() {
    assertThat(registry.hasType(StorageConnector.TYPE)).isTrue();
  }

  @Test
  void putGet_roundTrip() {
    String bucket = "pp-w1-us07-" + UUID.randomUUID().toString().substring(0, 8);
    String key = "fixtures/hello.txt";
    byte[] payload = ("hello-localstack-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);

    StorageConnector connector = new StorageConnector();
    connector.configure(
        new ConnectorContext("tenant-a", "conn-storage", null, null, null),
        new ConnectorConfig(
            Map.of(
                StorageConnector.PROP_BUCKET,
                bucket,
                "endpoint",
                ENDPOINT,
                "region",
                LocalStackS3ClientFactory.DEFAULT_REGION,
                StorageConnector.PROP_CREATE_BUCKET,
                true),
            Map.of()));

    try {
      ConnectionTestResult test = connector.testConnection();
      assertThat(test.success()).as(test.message()).isTrue();

      ConnectorResponse written =
          connector.write(
              new ConnectorRequest("rec-1", Map.of(StorageConnector.META_KEY, key), payload));
      assertThat(written.success()).as(written.errorMessage()).isTrue();

      ConnectorResponse read =
          connector.read(new ConnectorRequest("rec-1", Map.of(StorageConnector.META_KEY, key), null));
      assertThat(read.success()).as(read.errorMessage()).isTrue();
      assertThat(read.payload()).isEqualTo(payload);
    } finally {
      connector.close();
    }
  }
}
