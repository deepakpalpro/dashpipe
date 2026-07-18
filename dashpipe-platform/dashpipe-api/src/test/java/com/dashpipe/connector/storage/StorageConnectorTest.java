package com.dashpipe.connector.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.dashpipe.connector.spi.ConnectionTestResult;
import com.dashpipe.connector.spi.ConnectorConfig;
import com.dashpipe.connector.spi.ConnectorContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StorageConnectorTest {

  @Test
  void getType_isStorage() {
    assertThat(new StorageConnector().getType()).isEqualTo("storage");
  }

  @Test
  void testConnection_withoutConfig_failsCleanly() {
    StorageConnector connector = new StorageConnector();

    ConnectionTestResult result = connector.testConnection();

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("not configured");
  }

  @Test
  void testConnection_missingBucket_failsCleanly() {
    StorageConnector connector = new StorageConnector();
    connector.configure(
        new ConnectorContext("t1", "c1", null, null, null),
        new ConnectorConfig(
            Map.of(
                "endpoint", LocalStackS3ClientFactory.DEFAULT_ENDPOINT,
                "region", LocalStackS3ClientFactory.DEFAULT_REGION),
            Map.of()));

    ConnectionTestResult result = connector.testConnection();

    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("bucket");
    connector.close();
  }
}
