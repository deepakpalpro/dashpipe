package com.dashpipe.api.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
 * W1-US01 tenant CRUD + stub {@code X-Tenant-Id} context against Compose MySQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class TenantControllerIT {

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
  void createAndGet_roundTrip() {
    String slug = "demo-" + UUID.randomUUID().toString().substring(0, 8);
    CreateTenantRequest create =
        new CreateTenantRequest("Demo Tenant", slug, TenantStatus.trial);

    ResponseEntity<TenantResponse> created =
        restTemplate.postForEntity("/api/v1/tenants", create, TenantResponse.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    assertThat(created.getBody().id()).isNotBlank();
    assertThat(created.getBody().slug()).isEqualTo(slug);
    assertThat(created.getBody().name()).isEqualTo("Demo Tenant");
    assertThat(created.getBody().status()).isEqualTo(TenantStatus.trial);

    ResponseEntity<TenantResponse> fetched =
        restTemplate.getForEntity("/api/v1/tenants/" + created.getBody().id(), TenantResponse.class);

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody()).isNotNull();
    assertThat(fetched.getBody().id()).isEqualTo(created.getBody().id());
    assertThat(fetched.getBody().slug()).isEqualTo(slug);
    assertThat(fetched.getBody().name()).isEqualTo("Demo Tenant");
  }

  @Test
  void request_populatesTenantContext() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(TenantContextFilter.TENANT_ID_HEADER, "T001");

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/tenants/_context",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("tenantId")).isEqualTo("T001");
  }

  @Test
  void create_duplicateSlug_returnsConflict() {
    String slug = "dup-" + UUID.randomUUID().toString().substring(0, 8);
    CreateTenantRequest create = new CreateTenantRequest("First", slug, TenantStatus.active);

    ResponseEntity<TenantResponse> first =
        restTemplate.postForEntity("/api/v1/tenants", create, TenantResponse.class);
    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<String> second =
        restTemplate.postForEntity(
            "/api/v1/tenants",
            new HttpEntity<>(new CreateTenantRequest("Second", slug, TenantStatus.trial), headers),
            String.class);

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }
}
