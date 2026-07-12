package com.dashflow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.observability.PipeletMetricsEmitter;
import java.net.Socket;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * W0-US04 / W4-US01: Prometheus scrape endpoint against Compose MySQL ({@code local} profile).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class PrometheusEndpointIT {

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
  @Autowired private PipeletMetricsEmitter pipeletMetricsEmitter;

  @Test
  void prometheus_containsJvmMemoryMetric() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/actuator/prometheus", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("jvm_memory_used_bytes");
  }

  @Test
  void prometheus_containsPipeletMetrics_afterEmit() {
    pipeletMetricsEmitter.recordBatch(
        "T001", "pipe-fixture", "plet-fixture", 3, 3, Duration.ofMillis(2));

    ResponseEntity<String> response =
        restTemplate.getForEntity("/actuator/prometheus", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains(PipeletMetricsEmitter.RECORDS_IN);
    assertThat(response.getBody()).contains(PipeletMetricsEmitter.RECORDS_OUT);
    assertThat(response.getBody()).contains(PipeletMetricsEmitter.PROCESSING_DURATION);
    assertThat(response.getBody()).contains("tenant_id=\"T001\"");
    assertThat(response.getBody()).contains("pipeline_id=\"pipe-fixture\"");
    assertThat(response.getBody()).contains("pipelet_id=\"plet-fixture\"");
  }
}
