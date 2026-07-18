package com.dashpipe.api.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.api.connector.CreateConnectorRequest;
import com.dashpipe.api.connector.TenantConnectorResponse;
import com.dashpipe.api.tenant.CreateTenantRequest;
import com.dashpipe.api.tenant.TenantContextFilter;
import com.dashpipe.api.tenant.TenantResponse;
import com.dashpipe.api.tenant.TenantStatus;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

/** W3-US04: burst over per-tenant limit → 429. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "pipeline.webhook.rate-limit.enabled=true",
      "pipeline.webhook.rate-limit.requests-per-window=3",
      "pipeline.webhook.rate-limit.window-seconds=60",
      "pipeline.webhook.rate-limit.retry-after-seconds=15"
    })
@ActiveProfiles("local")
class WebhookRateLimitIT {

  private static final String SIGNING_SECRET = "test-secret";

  @BeforeAll
  static void requireComposeDeps() {
    assumeTrue(
        isPortOpen("127.0.0.1", 3306),
        "Compose MySQL is not reachable on localhost:3306 — run: docker compose up -d mysql");
    assumeTrue(
        isPortOpen("127.0.0.1", 5672),
        "Compose RabbitMQ is not reachable on localhost:5672 — run: docker compose up -d rabbitmq");
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
  @Autowired private WebhookRateLimiter rateLimiter;
  @Autowired private com.dashpipe.api.config.WebhookRateLimitProperties rateLimitProperties;

  @BeforeEach
  void clearLimiter() {
    rateLimiter.clear();
    assertThat(rateLimitProperties.isEnabled()).isTrue();
    assertThat(rateLimitProperties.getRequestsPerWindow()).isEqualTo(3);
  }

  @Test
  void burst_exceedsLimit_returns429() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("WhRL " + suffix, "whrl-" + suffix);
    TenantConnectorResponse connector =
        createEventListenerConnector(tenant.id(), "github-rl-" + suffix);

    String rawJson = "{\"action\":\"opened\"}";
    byte[] rawBytes = rawJson.getBytes(StandardCharsets.UTF_8);
    String signature =
        "sha256=" + WebhookSignatureVerifier.hmacSha256Hex(SIGNING_SECRET, rawBytes);
    HttpHeaders headers = signedJsonHeaders(signature);

    HttpStatus last = null;
    for (int i = 0; i < 4; i++) {
      headers.set(WebhookIdempotencyService.WEBHOOK_ID_HEADER, "rl-" + suffix + "-" + i);
      ResponseEntity<String> response =
          restTemplate.exchange(
              "/api/v1/webhooks/" + tenant.id() + "/" + connector.id(),
              HttpMethod.POST,
              new HttpEntity<>(rawBytes, headers),
              String.class);
      last = HttpStatus.valueOf(response.getStatusCode().value());
      if (i < 3) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
      }
    }

    assertThat(last).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  private TenantConnectorResponse createEventListenerConnector(String tenantId, String name) {
    CreateConnectorRequest request =
        new CreateConnectorRequest(
            "ct-event-listener",
            name,
            objectMapper
                .createObjectNode()
                .put("path_hint", "/hooks")
                .put("signing_secret", SIGNING_SECRET), null, null);
    ResponseEntity<TenantConnectorResponse> created =
        restTemplate.exchange(
            "/api/v1/connectors",
            HttpMethod.POST,
            new HttpEntity<>(request, jsonTenantHeaders(tenantId)),
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

  private static HttpHeaders signedJsonHeaders(String signature) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(WebhookSignatureVerifier.DEFAULT_SIGNATURE_HEADER, signature);
    return headers;
  }

  private static HttpHeaders jsonTenantHeaders(String tenantId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(TenantContextFilter.TENANT_ID_HEADER, tenantId);
    return headers;
  }
}
