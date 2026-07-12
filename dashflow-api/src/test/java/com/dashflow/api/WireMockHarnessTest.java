package com.dashflow.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * W0-US05: example HTTP stub harness for future Rest connector tests.
 */
class WireMockHarnessTest {

  @RegisterExtension
  static final WireMockExtension WIRE_MOCK =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Test
  void stub_returnsOk() throws IOException, InterruptedException {
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/external/ping"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"ok\":true}")));

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(WIRE_MOCK.baseUrl() + "/external/ping")).GET().build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("{\"ok\":true}");
    WIRE_MOCK.verify(getRequestedFor(urlEqualTo("/external/ping")));
  }
}
