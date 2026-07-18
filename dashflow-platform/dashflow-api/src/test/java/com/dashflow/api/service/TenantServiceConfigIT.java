package com.dashflow.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dashflow.api.tenant.CreateTenantRequest;
import com.dashflow.api.tenant.TenantContextFilter;
import com.dashflow.api.tenant.TenantResponse;
import com.dashflow.api.tenant.TenantStatus;
import java.net.Socket;
import java.util.Map;
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

/**
 * W1-US04: tenant Auth service config — merge defaults, redact secrets, isolate by tenant.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class TenantServiceConfigIT {

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
  void createAndGet_asTenant_redactsSecret() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Svc A " + suffix, "svc-a-" + suffix);

    ObjectNode config =
        objectMapper
            .createObjectNode()
            .put("client_id", "cid-" + suffix)
            .put("client_secret", "raw-secret-value");

    CreateTenantServiceRequest create =
        new CreateTenantServiceRequest(
            ServiceTypeService.AUTH_TYPE_ID,
            ServiceTypeService.STUB_AUTH_VENDOR,
            "T-Auth-" + suffix,
            config,
            null,
            null,
            true
        );

    ResponseEntity<TenantServiceResponse> created =
        restTemplate.exchange(
            "/api/v1/services",
            HttpMethod.POST,
            new HttpEntity<>(create, jsonTenantHeaders(tenant.id())),
            TenantServiceResponse.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    assertThat(created.getBody().config().get("client_secret").asText())
        .isEqualTo(SecretRedactor.REDACTED);
    assertThat(created.getBody().config().get("client_id").asText()).isEqualTo("cid-" + suffix);
    // inherited from StubAuth defaults
    assertThat(created.getBody().config().get("issuer").asText()).contains("auth.example.local");

    ResponseEntity<TenantServiceResponse> fetched =
        restTemplate.exchange(
            "/api/v1/services/" + created.getBody().id(),
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            TenantServiceResponse.class);

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody()).isNotNull();
    assertThat(fetched.getBody().config().toString()).doesNotContain("raw-secret-value");
    assertThat(fetched.getBody().config().get("client_secret").asText())
        .isEqualTo(SecretRedactor.REDACTED);
  }

  @Test
  void get_asOtherTenant_returnsNotFound() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("Iso A " + suffix, "iso-a-" + suffix);
    TenantResponse tenantB = createTenant("Iso B " + suffix, "iso-b-" + suffix);

    ObjectNode config = objectMapper.createObjectNode().put("client_id", "only-b");
    TenantServiceResponse created =
        restTemplate
            .exchange(
                "/api/v1/services",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreateTenantServiceRequest(
            ServiceTypeService.AUTH_TYPE_ID,
            ServiceTypeService.STUB_AUTH_VENDOR,
            "B-Auth-" + suffix,
            config,
            null,
            null,
            true
        ),
                    jsonTenantHeaders(tenantB.id())),
                TenantServiceResponse.class)
            .getBody();
    assertThat(created).isNotNull();

    ResponseEntity<String> asA =
        restTemplate.exchange(
            "/api/v1/services/" + created.id(),
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenantA.id())),
            String.class);

    assertThat(asA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
