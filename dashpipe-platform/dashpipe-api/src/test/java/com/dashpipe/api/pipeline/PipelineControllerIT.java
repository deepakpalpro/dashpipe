package com.dashpipe.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashpipe.api.tenant.CreateTenantRequest;
import com.dashpipe.api.tenant.TenantContextFilter;
import com.dashpipe.api.tenant.TenantResponse;
import com.dashpipe.api.tenant.TenantStatus;
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

/** W2-US01: pipeline CRUD + tenant isolation against Compose MySQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class PipelineControllerIT {

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
  void createAndGet_asTenant() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Pipe A " + suffix, "pipe-a-" + suffix);

    CreatePipelineRequest create =
        new CreatePipelineRequest(
            "customer-sync-" + suffix,
            "Sync customers",
            PipelineVisibility.PRIVATE,
            PipelineExecutionMode.ASYNC, null, null);

    ResponseEntity<PipelineResponse> created =
        restTemplate.exchange(
            "/api/v1/pipelines",
            HttpMethod.POST,
            new HttpEntity<>(create, jsonTenantHeaders(tenant.id())),
            PipelineResponse.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    assertThat(created.getBody().id()).isNotBlank();
    assertThat(created.getBody().name()).isEqualTo("customer-sync-" + suffix);
    assertThat(created.getBody().visibility()).isEqualTo(PipelineVisibility.PRIVATE);
    assertThat(created.getBody().executionMode()).isEqualTo(PipelineExecutionMode.ASYNC);
    assertThat(created.getBody().status()).isEqualTo(PipelineStatus.DRAFT);
    assertThat(created.getBody().version()).isEqualTo(1);
    assertThat(created.getBody().steps()).isEmpty();

    ResponseEntity<PipelineResponse> fetched =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.getBody().id(),
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            PipelineResponse.class);

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody()).isNotNull();
    assertThat(fetched.getBody().id()).isEqualTo(created.getBody().id());
    assertThat(fetched.getBody().tenantId()).isEqualTo(tenant.id());
  }

  @Test
  void get_asOtherTenant_returnsNotFound() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("Iso A " + suffix, "iso-a-" + suffix);
    TenantResponse tenantB = createTenant("Iso B " + suffix, "iso-b-" + suffix);

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest("only-b-" + suffix, null, null, null, null, null),
                    jsonTenantHeaders(tenantB.id())),
                PipelineResponse.class)
            .getBody();
    assertThat(created).isNotNull();

    ResponseEntity<String> asA =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id(),
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenantA.id())),
            String.class);

    assertThat(asA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void delete_archivesPipeline() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Arch " + suffix, "arch-" + suffix);

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest("to-archive-" + suffix, null, null, null, null, null),
                    jsonTenantHeaders(tenant.id())),
                PipelineResponse.class)
            .getBody();
    assertThat(created).isNotNull();

    ResponseEntity<PipelineResponse> archived =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id(),
            HttpMethod.DELETE,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            PipelineResponse.class);

    assertThat(archived.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(archived.getBody()).isNotNull();
    assertThat(archived.getBody().status()).isEqualTo(PipelineStatus.ARCHIVED);
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
