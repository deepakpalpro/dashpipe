package com.dashpipe.api.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ConnectorTypeControllerIT {

  @BeforeAll
  static void requireComposeMysql() {
    assumeTrue(
        isPortOpen("127.0.0.1", 3306),
        "Compose MySQL is not reachable on localhost:3306 — run: docker compose up -d mysql");
  }

  private static boolean isPortOpen(String host, int port) {
    try (Socket socket = new Socket(host, port)) {
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void list_includesRestType() {
    ResponseEntity<List<Map<String, Object>>> response =
        restTemplate.exchange(
            "/api/v1/connector-types",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody())
        .anySatisfy(
            row -> {
              assertThat(row.get("type")).isEqualTo("rest");
              assertThat(row.get("spiClass"))
                  .isEqualTo("com.dashpipe.connector.rest.RestConnector");
            });
    assertThat(response.getBody())
        .anySatisfy(
            row -> {
              assertThat(row.get("type")).isEqualTo("storage");
              assertThat(row.get("spiClass"))
                  .isEqualTo("com.dashpipe.connector.storage.StorageConnector");
            });
    assertThat(response.getBody())
        .anySatisfy(
            row -> {
              assertThat(row.get("type")).isEqualTo("message_bus");
              assertThat(row.get("spiClass"))
                  .isEqualTo("com.dashpipe.connector.messagebus.MessageBusConnector");
            });
  }
}
