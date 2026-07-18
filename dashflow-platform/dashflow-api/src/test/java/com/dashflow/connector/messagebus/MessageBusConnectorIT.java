package com.dashflow.connector.messagebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.PipelineApiApplication;
import com.dashflow.connector.ConnectorRegistry;
import com.dashflow.connector.localstack.LocalStackAwsClientFactory;
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
 * W1-US08: MessageBusConnector publish/receive against Compose LocalStack SQS.
 */
@SpringBootTest(classes = PipelineApiApplication.class)
@ActiveProfiles("local")
class MessageBusConnectorIT {

  private static final String ENDPOINT =
      System.getenv()
          .getOrDefault("LOCALSTACK_ENDPOINT", LocalStackAwsClientFactory.DEFAULT_ENDPOINT);

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
  void registry_includesMessageBus() {
    assertThat(registry.hasType(MessageBusConnector.TYPE)).isTrue();
  }

  @Test
  void publish_succeeds() {
    String queueName = "pp-w1-us08-" + UUID.randomUUID().toString().substring(0, 8);
    byte[] payload = ("hello-sqs-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);

    MessageBusConnector connector = new MessageBusConnector();
    connector.configure(
        new ConnectorContext("tenant-a", "conn-mb", null, null, null),
        new ConnectorConfig(
            Map.of(
                MessageBusConnector.PROP_QUEUE_NAME,
                queueName,
                "endpoint",
                ENDPOINT,
                "region",
                LocalStackAwsClientFactory.DEFAULT_REGION,
                MessageBusConnector.PROP_CREATE_QUEUE,
                true),
            Map.of()));

    try {
      ConnectionTestResult test = connector.testConnection();
      assertThat(test.success()).as(test.message()).isTrue();

      ConnectorResponse published =
          connector.write(new ConnectorRequest("rec-1", Map.of(), payload));
      assertThat(published.success()).as(published.errorMessage()).isTrue();
      assertThat(published.metadata()).containsKey(MessageBusConnector.META_MESSAGE_ID);

      ConnectorResponse received = connector.read(new ConnectorRequest("rec-1", Map.of(), null));
      assertThat(received.success()).as(received.errorMessage()).isTrue();
      assertThat(received.payload()).isEqualTo(payload);
    } finally {
      connector.close();
    }
  }
}
