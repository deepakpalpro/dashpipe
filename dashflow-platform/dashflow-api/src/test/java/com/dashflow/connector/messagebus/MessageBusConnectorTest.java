package com.dashflow.connector.messagebus;

import static org.assertj.core.api.Assertions.assertThat;

import com.dashflow.connector.localstack.LocalStackAwsClientFactory;
import com.dashflow.connector.spi.ConnectionTestResult;
import com.dashflow.connector.spi.ConnectorConfig;
import com.dashflow.connector.spi.ConnectorContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageBusConnectorTest {

  @Test
  void getType_isMessageBus() {
    assertThat(new MessageBusConnector().getType()).isEqualTo("message_bus");
  }

  @Test
  void testConnection_withoutConfig_failsCleanly() {
    MessageBusConnector connector = new MessageBusConnector();

    ConnectionTestResult result = connector.testConnection();

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("not configured");
  }

  @Test
  void rewriteLocalStackUrl_mapsContainerHostToConfiguredEndpoint() {
    String rewritten =
        LocalStackAwsClientFactory.rewriteLocalStackUrl(
            "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/q1",
            "http://localhost:4567");

    assertThat(rewritten).isEqualTo("http://localhost:4567/000000000000/q1");
  }

  @Test
  void testConnection_missingQueue_failsCleanly() {
    MessageBusConnector connector = new MessageBusConnector();
    connector.configure(
        new ConnectorContext("t1", "c1", null, null, null),
        new ConnectorConfig(
            Map.of(
                "endpoint", LocalStackAwsClientFactory.DEFAULT_ENDPOINT,
                "region", LocalStackAwsClientFactory.DEFAULT_REGION),
            Map.of()));

    ConnectionTestResult result = connector.testConnection();

    assertThat(result.success()).isFalse();
    connector.close();
  }
}
