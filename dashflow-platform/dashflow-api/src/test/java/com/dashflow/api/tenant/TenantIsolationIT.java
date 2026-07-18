package com.dashflow.api.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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

/**
 * W1-US02: tenant A must not read tenant B's owned rows (Hibernate tenant filter).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class TenantIsolationIT {

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
  void tenantA_cannotReadTenantB() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("Tenant A " + suffix, "ten-a-" + suffix);
    TenantResponse tenantB = createTenant("Tenant B " + suffix, "ten-b-" + suffix);

    TenantNoteResponse noteB =
        createNote(tenantB.id(), new CreateTenantNoteRequest("B secret", "owned by B"));

    ResponseEntity<String> asA =
        restTemplate.exchange(
            "/api/v1/tenant-notes/" + noteB.id(),
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenantA.id())),
            String.class);

    assertThat(asA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(asA.getBody() == null ? "" : asA.getBody()).doesNotContain("B secret");
  }

  @Test
  void tenantB_canReadOwnNote() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantB = createTenant("Tenant B2 " + suffix, "ten-b2-" + suffix);

    TenantNoteResponse noteB =
        createNote(tenantB.id(), new CreateTenantNoteRequest("B visible", "body"));

    ResponseEntity<TenantNoteResponse> asB =
        restTemplate.exchange(
            "/api/v1/tenant-notes/" + noteB.id(),
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenantB.id())),
            TenantNoteResponse.class);

    assertThat(asB.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(asB.getBody()).isNotNull();
    assertThat(asB.getBody().title()).isEqualTo("B visible");
    assertThat(asB.getBody().tenantId()).isEqualTo(tenantB.id());
  }

  @Test
  void list_onlyReturnsCurrentTenantNotes() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("List A " + suffix, "list-a-" + suffix);
    TenantResponse tenantB = createTenant("List B " + suffix, "list-b-" + suffix);

    createNote(tenantA.id(), new CreateTenantNoteRequest("A note", "a"));
    createNote(tenantB.id(), new CreateTenantNoteRequest("B note", "b"));

    ResponseEntity<TenantNoteResponse[]> asA =
        restTemplate.exchange(
            "/api/v1/tenant-notes",
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenantA.id())),
            TenantNoteResponse[].class);

    assertThat(asA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(asA.getBody()).isNotNull();
    assertThat(asA.getBody())
        .allSatisfy(n -> assertThat(n.tenantId()).isEqualTo(tenantA.id()));
    assertThat(asA.getBody()).noneMatch(n -> "B note".equals(n.title()));
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

  private TenantNoteResponse createNote(String tenantId, CreateTenantNoteRequest body) {
    HttpHeaders headers = tenantHeaders(tenantId);
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<TenantNoteResponse> created =
        restTemplate.exchange(
            "/api/v1/tenant-notes",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            TenantNoteResponse.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    return created.getBody();
  }

  private static HttpHeaders tenantHeaders(String tenantId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(TenantContextFilter.TENANT_ID_HEADER, tenantId);
    return headers;
  }
}
