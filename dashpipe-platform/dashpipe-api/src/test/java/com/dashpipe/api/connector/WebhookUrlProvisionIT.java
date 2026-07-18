package com.dashpipe.api.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dashpipe.api.PipelineApiApplication;
import com.dashpipe.api.service.SecretEncryptor;
import com.dashpipe.api.tenant.CreateTenantRequest;
import com.dashpipe.api.tenant.TenantContextFilter;
import com.dashpipe.api.tenant.TenantResponse;
import com.dashpipe.api.tenant.TenantStatus;
import com.dashpipe.api.webhook.WebhookSignatureVerifier;
import java.net.Socket;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

/** W3-US05: POST /api/v1/connectors/{id}/webhook-url */
@SpringBootTest(
    classes = PipelineApiApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class WebhookUrlProvisionIT {

  private static final String SIGNING_SECRET = "provision-secret";

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
  void provision_returnsStableUrl() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("WhUrl " + suffix, "whurl-" + suffix);
    TenantConnectorResponse connector =
        createEventListener(tenant.id(), "github-" + suffix, SIGNING_SECRET);

    ResponseEntity<WebhookUrlProvisionResponse> first = provision(tenant.id(), connector.id());
    ResponseEntity<WebhookUrlProvisionResponse> second = provision(tenant.id(), connector.id());

    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(first.getBody()).isNotNull();
    String expectedPath = "/api/v1/webhooks/" + tenant.id() + "/" + connector.id();
    assertThat(first.getBody().webhookUrl()).endsWith(expectedPath);
    assertThat(first.getBody().signingSecret()).isEqualTo(SecretEncryptor.PREFIX + SIGNING_SECRET);
    assertThat(first.getBody().signatureHeader())
        .isEqualTo(WebhookSignatureVerifier.DEFAULT_SIGNATURE_HEADER);
    assertThat(first.getBody().createdAt()).isNotNull();

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getBody()).isNotNull();
    assertThat(second.getBody().webhookUrl()).isEqualTo(first.getBody().webhookUrl());
  }

  @Test
  void provision_asOtherTenant_returnsNotFound() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("WhA " + suffix, "wha-" + suffix);
    TenantResponse tenantB = createTenant("WhB " + suffix, "whb-" + suffix);
    TenantConnectorResponse connector =
        createEventListener(tenantB.id(), "b-github-" + suffix, SIGNING_SECRET);

    ResponseEntity<String> asA =
        restTemplate.exchange(
            "/api/v1/connectors/" + connector.id() + "/webhook-url",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenantA.id())),
            String.class);

    assertThat(asA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void provision_nonEventListener_returnsBadRequest() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("WhRest " + suffix, "whrest-" + suffix);
    TenantConnectorResponse connector = createRestConnector(tenant.id(), "rest-" + suffix);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/api/v1/connectors/" + connector.id() + "/webhook-url",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private ResponseEntity<WebhookUrlProvisionResponse> provision(
      String tenantId, String connectorId) {
    return restTemplate.exchange(
        "/api/v1/connectors/" + connectorId + "/webhook-url",
        HttpMethod.POST,
        new HttpEntity<>(tenantHeaders(tenantId)),
        WebhookUrlProvisionResponse.class);
  }

  private TenantConnectorResponse createEventListener(
      String tenantId, String name, String signingSecret) {
    ObjectNode config =
        objectMapper
            .createObjectNode()
            .put("path_hint", "/hooks")
            .put("signing_secret", signingSecret);

    ResponseEntity<TenantConnectorResponse> created =
        restTemplate.exchange(
            "/api/v1/connectors",
            HttpMethod.POST,
            new HttpEntity<>(
                new CreateConnectorRequest(
                    WebhookUrlProvisionService.EVENT_LISTENER_TYPE_ID, name, config, null, null),
                jsonTenantHeaders(tenantId)),
            TenantConnectorResponse.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    return created.getBody();
  }

  private TenantConnectorResponse createRestConnector(String tenantId, String name) {
    ObjectNode config =
        objectMapper
            .createObjectNode()
            .put("baseUrl", "http://127.0.0.1:9")
            .put("pingPath", "/external/ping");

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
