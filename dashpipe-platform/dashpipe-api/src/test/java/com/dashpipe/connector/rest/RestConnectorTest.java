package com.dashpipe.connector.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.dashpipe.connector.spi.ConnectionTestResult;
import com.dashpipe.connector.spi.ConnectorConfig;
import com.dashpipe.connector.spi.ConnectorContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RestConnectorTest {

  @RegisterExtension
  static final WireMockExtension WIRE_MOCK =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Test
  void getType_isRest() {
    assertThat(new RestConnector().getType()).isEqualTo("rest");
  }

  @Test
  void testConnection_withoutConfig_failsCleanly() {
    RestConnector connector = new RestConnector();

    ConnectionTestResult result = connector.testConnection();

    assertThat(result.success()).isFalse();
    assertThat(result.message()).containsIgnoringCase("not configured");
  }

  @Test
  void testConnection_mapsHttpFailure() {
    WIRE_MOCK.stubFor(
        get(urlEqualTo(RestConnector.DEFAULT_PING_PATH))
            .willReturn(aResponse().withStatus(500).withBody("boom")));

    RestConnector connector = new RestConnector();
    connector.configure(
        new ConnectorContext("t1", "c1", null, null, null),
        new ConnectorConfig(Map.of(RestConnector.PROP_BASE_URL, WIRE_MOCK.baseUrl()), Map.of()));

    ConnectionTestResult result = connector.testConnection();

    assertThat(result.success()).isFalse();
    assertThat(result.message()).contains("HTTP 500");
    WIRE_MOCK.verify(getRequestedFor(urlEqualTo(RestConnector.DEFAULT_PING_PATH)));
  }

  @Test
  void testConnection_success_againstWireMock() {
    WIRE_MOCK.stubFor(
        get(urlEqualTo(RestConnector.DEFAULT_PING_PATH))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"ok\":true}")));

    RestConnector connector = new RestConnector();
    connector.configure(
        new ConnectorContext("t1", "c1", null, null, null),
        new ConnectorConfig(Map.of(RestConnector.PROP_BASE_URL, WIRE_MOCK.baseUrl()), Map.of()));

    ConnectionTestResult result = connector.testConnection();

    assertThat(result.success()).isTrue();
    assertThat(result.message()).isEqualTo("Connection successful");
    assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void joinUrl_stripsTrailingSlash() {
    assertThat(RestConnector.joinUrl("http://localhost:8089/", "/external/ping").toString())
        .isEqualTo("http://localhost:8089/external/ping");
  }
}
