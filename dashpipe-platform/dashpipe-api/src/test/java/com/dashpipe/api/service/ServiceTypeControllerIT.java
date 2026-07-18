package com.dashpipe.api.service;

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

/**
 * W1-US03: platform service-type catalog seeded by Flyway (global, not tenant-scoped).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ServiceTypeControllerIT {

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
  void list_returnsCatalog() {
    ResponseEntity<List<Map<String, Object>>> response =
        restTemplate.exchange(
            "/api/v1/service-types",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull().isNotEmpty();

    Map<String, Object> auth =
        response.getBody().stream()
            .filter(row -> "auth".equals(String.valueOf(row.get("type"))))
            .findFirst()
            .orElseThrow();

    assertThat(auth.get("id")).isEqualTo("st-auth");
    assertThat(auth.get("displayName")).isEqualTo("Authentication");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> defaults = (List<Map<String, Object>>) auth.get("defaults");
    assertThat(defaults).isNotEmpty();
    assertThat(defaults)
        .extracting(d -> String.valueOf(d.get("vendor")))
        .contains(
            "StubAuth",
            "OAuth",
            "OIDC",
            "Keycloak",
            "AAD",
            "AWSCognito",
            "AzureMI",
            "CertBased",
            "JWT");
    assertThat(defaults.getFirst().get("defaultConfig")).isInstanceOf(Map.class);
  }
}
