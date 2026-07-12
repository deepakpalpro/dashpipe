package com.dashflow.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.tenant.CreateTenantRequest;
import com.dashflow.api.tenant.TenantContextFilter;
import com.dashflow.api.tenant.TenantResponse;
import com.dashflow.api.tenant.TenantStatus;
import java.net.Socket;
import java.util.List;
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

/** W6 dry-run: validate pipeline without side effects. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class PipelineDryRunIT {

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
  void dryRun_withSteps_returnsValid() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Dry " + suffix, "dry-" + suffix);

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest("dry-run-" + suffix, null, null, null, null, null),
                    jsonTenantHeaders(tenant.id())),
                PipelineResponse.class)
            .getBody();
    assertThat(created).isNotNull();

    Map<String, Object> stepsBody =
        Map.of(
            "steps",
            List.of(
                Map.of(
                    "pipelet_id",
                    "plet-rest-source",
                    "step_order",
                    1,
                    "connector_ids",
                    List.of(),
                    "service_ids",
                    List.of())));

    ResponseEntity<PipelineResponse> put =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id() + "/steps",
            HttpMethod.PUT,
            new HttpEntity<>(stepsBody, jsonTenantHeaders(tenant.id())),
            PipelineResponse.class);
    assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(put.getBody()).isNotNull();
    assertThat(put.getBody().status()).isEqualTo(PipelineStatus.ACTIVE);

    ResponseEntity<PipelineDryRunResponse> dry =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id() + "/dry-run",
            HttpMethod.POST,
            new HttpEntity<>(null, jsonTenantHeaders(tenant.id())),
            PipelineDryRunResponse.class);

    assertThat(dry.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(dry.getBody()).isNotNull();
    assertThat(dry.getBody().valid()).isTrue();
    assertThat(dry.getBody().messages()).isNotEmpty();
  }

  @Test
  void dryRun_withoutSteps_returnsInvalid() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("DryEmpty " + suffix, "drye-" + suffix);

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest("empty-" + suffix, null, null, null, null, null),
                    jsonTenantHeaders(tenant.id())),
                PipelineResponse.class)
            .getBody();
    assertThat(created).isNotNull();

    ResponseEntity<PipelineDryRunResponse> dry =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id() + "/dry-run",
            HttpMethod.POST,
            new HttpEntity<>(null, jsonTenantHeaders(tenant.id())),
            PipelineDryRunResponse.class);

    assertThat(dry.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(dry.getBody()).isNotNull();
    assertThat(dry.getBody().valid()).isFalse();
    assertThat(dry.getBody().messages().get(0)).contains("no steps");
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

  private static HttpHeaders jsonTenantHeaders(String tenantId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(TenantContextFilter.TENANT_ID_HEADER, tenantId);
    return headers;
  }
}
