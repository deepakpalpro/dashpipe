package com.dashflow.connector.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.dashflow.api.PipelineApiApplication;
import com.dashflow.api.connector.ConnectionTestResponse;
import com.dashflow.api.connector.CreateConnectorRequest;
import com.dashflow.api.connector.TenantConnectorResponse;
import com.dashflow.api.connector.TenantConnectorService;
import com.dashflow.api.tenant.CreateTenantRequest;
import com.dashflow.api.tenant.TenantContextFilter;
import com.dashflow.api.tenant.TenantResponse;
import com.dashflow.api.tenant.TenantStatus;
import java.net.Socket;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * W1-US06: POST /api/v1/connectors/{id}/test against WireMock /external/ping.
 */
@SpringBootTest(
    classes = PipelineApiApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class RestConnectorTestIT {

  @RegisterExtension
  static final WireMockExtension WIRE_MOCK =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

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
  @Autowired private ObjectMapper objectMapper;

  @Test
  void test_returnsSuccess_againstWireMock() {
    WIRE_MOCK.stubFor(
        get(urlEqualTo(RestConnector.DEFAULT_PING_PATH))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"ok\":true}")));

    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Conn A " + suffix, "conn-a-" + suffix);
    TenantConnectorResponse connector = createRestConnector(tenant.id(), "Rest-" + suffix);

    ResponseEntity<ConnectionTestResponse> tested =
        restTemplate.exchange(
            "/api/v1/connectors/" + connector.id() + "/test",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            ConnectionTestResponse.class);

    assertThat(tested.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(tested.getBody()).isNotNull();
    assertThat(tested.getBody().success()).isTrue();
    assertThat(tested.getBody().message()).isEqualTo("Connection successful");
    assertThat(tested.getBody().latencyMs()).isGreaterThanOrEqualTo(0);
    assertThat(tested.getBody().testedAt()).isNotNull();

    WIRE_MOCK.verify(getRequestedFor(urlEqualTo(RestConnector.DEFAULT_PING_PATH)));
  }

  @Test
  void test_asOtherTenant_returnsNotFound() {
    WIRE_MOCK.stubFor(
        get(urlEqualTo(RestConnector.DEFAULT_PING_PATH))
            .willReturn(aResponse().withStatus(200).withBody("{\"ok\":true}")));

    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("Iso A " + suffix, "iso-a-" + suffix);
    TenantResponse tenantB = createTenant("Iso B " + suffix, "iso-b-" + suffix);
    TenantConnectorResponse connector = createRestConnector(tenantB.id(), "B-Rest-" + suffix);

    ResponseEntity<String> asA =
        restTemplate.exchange(
            "/api/v1/connectors/" + connector.id() + "/test",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenantA.id())),
            String.class);

    assertThat(asA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private TenantConnectorResponse createRestConnector(String tenantId, String name) {
    ObjectNode config =
        objectMapper
            .createObjectNode()
            .put(RestConnector.PROP_BASE_URL, WIRE_MOCK.baseUrl())
            .put(RestConnector.PROP_PING_PATH, RestConnector.DEFAULT_PING_PATH);

    ResponseEntity<TenantConnectorResponse> created =
        restTemplate.exchange(
            "/api/v1/connectors",
            HttpMethod.POST,
            new HttpEntity<>(
                new CreateConnectorRequest(TenantConnectorService.REST_TYPE_ID, name, config, null, null),
                jsonTenantHeaders(tenantId)),
            TenantConnectorResponse.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    return created.getBody();
  }

  private TenantResponse createTenant(String name, String slug) {
    ResponseEntity<TenantResponse> created =
        restTemplate.postForEntity(
            "/api/v1/tenants",
            new CreateTenantRequest(name, slug, TenantStatus.active),
            TenantResponse.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    return created.getBody();
  }

  private static HttpHeaders tenantHeaders(String tenantId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(TenantContextFilter.TENANT_ID_HEADER, tenantId);
    return headers;
  }

  private static HttpHeaders jsonTenantHeaders(String tenantId) {
    HttpHeaders headers = tenantHeaders(tenantId);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
