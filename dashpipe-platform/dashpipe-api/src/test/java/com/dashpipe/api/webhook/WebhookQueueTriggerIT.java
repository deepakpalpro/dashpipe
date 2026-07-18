package com.dashpipe.api.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.api.connector.CreateConnectorRequest;
import com.dashpipe.api.connector.TenantConnectorResponse;
import com.dashpipe.api.k8s.StubPipeletJobClient;
import com.dashpipe.api.messaging.QueueNaming;
import com.dashpipe.api.tenant.CreateTenantRequest;
import com.dashpipe.api.tenant.TenantContextFilter;
import com.dashpipe.api.tenant.TenantResponse;
import com.dashpipe.api.tenant.TenantStatus;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
import org.springframework.test.context.TestPropertySource;

/**
 * W3-US06: publish → queue depth &gt; 0 → stub PipeletJobClient create (poller enabled).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@TestPropertySource(
    properties = {
      "pipeline.webhook.queue-trigger.enabled=true",
      "pipeline.webhook.queue-trigger.poll-interval-ms=200"
    })
class WebhookQueueTriggerIT {

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
  @Autowired private StubPipeletJobClient stubPipeletJobClient;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private WebhookProcessorTrigger webhookProcessorTrigger;
  @Autowired private WebhookQueueWatchRegistry queueWatchRegistry;

  @BeforeEach
  void reset() {
    stubPipeletJobClient.clear();
    webhookProcessorTrigger.clearBusy();
    queueWatchRegistry.clear();
  }

  @Test
  void depth_triggersJob() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("WhTrig " + suffix, "whtrig-" + suffix);
    TenantConnectorResponse connector =
        createEventListenerConnector(tenant.id(), "github-trig-" + suffix);

    String rawJson = "{\"action\":\"opened\"}";
    byte[] rawBytes = rawJson.getBytes(StandardCharsets.UTF_8);
    String signature =
        "sha256=" + WebhookSignatureVerifier.hmacSha256Hex(SIGNING_SECRET, rawBytes);

    ResponseEntity<WebhookAcceptResponse> response =
        restTemplate.exchange(
            "/api/v1/webhooks/" + tenant.id() + "/" + connector.id(),
            HttpMethod.POST,
            new HttpEntity<>(rawBytes, signedJsonHeaders(signature)),
            WebhookAcceptResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    // Leave message on queue so depth stays > 0 for the poller.
    assertThat(stubPipeletJobClient.getCreated()).isEmpty();

    await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(100))
        .untilAsserted(
            () ->
                assertThat(stubPipeletJobClient.getCreated())
                    .anySatisfy(
                        req -> {
                          assertThat(req.tenantId()).isEqualTo(tenant.id());
                          assertThat(req.inputQueue())
                              .isEqualTo(
                                  QueueNaming.webhookInputQueue(tenant.id(), connector.id()));
                          assertThat(req.pipeletId())
                              .isEqualTo(WebhookProcessorTrigger.WEBHOOK_PROCESSOR_PIPELET_ID);
                        }));
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
