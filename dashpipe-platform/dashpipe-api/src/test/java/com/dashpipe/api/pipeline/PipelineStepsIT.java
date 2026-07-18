package com.dashpipe.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashpipe.api.tenant.CreateTenantRequest;
import com.dashpipe.api.tenant.TenantContextFilter;
import com.dashpipe.api.tenant.TenantResponse;
import com.dashpipe.api.tenant.TenantStatus;
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

/** W2-US02: replace pipeline steps + GET returns them (Compose MySQL). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class PipelineStepsIT {

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
  void putSteps_thenGetPipeline() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Steps " + suffix, "steps-" + suffix);

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest("with-steps-" + suffix, null, null, null, null, null),
                    jsonTenantHeaders(tenant.id())),
                PipelineResponse.class)
            .getBody();
    assertThat(created).isNotNull();
    assertThat(created.version()).isEqualTo(1);

    Map<String, Object> body =
        Map.of(
            "steps",
            List.of(
                Map.of(
                    "pipelet_id",
                    "plet-rest-source",
                    "step_order",
                    1,
                    "config",
                    Map.of("batch_size", 100),
                    "connector_ids",
                    List.of("conn-crm-rest"),
                    "service_ids",
                    List.of("svc-okta-auth"),
                    "input_queue",
                    "t." + suffix + ".s1.in",
                    "output_queue",
                    "t." + suffix + ".s1.out",
                    "resource_limits",
                    Map.of("cpu", "500m", "memory", "512Mi")),
                Map.of(
                    "pipelet_id",
                    "plet-json-transform",
                    "step_order",
                    2,
                    "config",
                    Map.of("field_mapping", Map.of("cust_id", "customer_id")),
                    "connector_ids",
                    List.of(),
                    "service_ids",
                    List.of()),
                Map.of(
                    "pipelet_id",
                    "plet-db-dest",
                    "step_order",
                    3,
                    "config",
                    Map.of("table", "customers"),
                    "connector_ids",
                    List.of("conn-warehouse-db"),
                    "service_ids",
                    List.of())));

    ResponseEntity<PipelineResponse> put =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id() + "/steps",
            HttpMethod.PUT,
            new HttpEntity<>(body, jsonTenantHeaders(tenant.id())),
            PipelineResponse.class);

    assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(put.getBody()).isNotNull();
    assertThat(put.getBody().version()).isEqualTo(2);
    assertThat(put.getBody().steps()).hasSize(3);
    assertThat(put.getBody().steps())
        .extracting(PipelineStepResponse::stepOrder)
        .containsExactly(1, 2, 3);
    assertThat(put.getBody().steps().get(0).pipeletId()).isEqualTo("plet-rest-source");
    assertThat(put.getBody().steps().get(0).connectorIds()).containsExactly("conn-crm-rest");
    assertThat(put.getBody().steps().get(0).inputQueue()).isEqualTo("t." + suffix + ".s1.in");

    ResponseEntity<PipelineResponse> fetched =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id(),
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            PipelineResponse.class);

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody()).isNotNull();
    assertThat(fetched.getBody().steps()).hasSize(3);
    assertThat(fetched.getBody().steps().get(1).pipeletId()).isEqualTo("plet-json-transform");
  }

  @Test
  void putSteps_asOtherTenant_returnsNotFound() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("Steps Iso A " + suffix, "siso-a-" + suffix);
    TenantResponse tenantB = createTenant("Steps Iso B " + suffix, "siso-b-" + suffix);

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest("owned-by-b-" + suffix, null, null, null, null, null),
                    jsonTenantHeaders(tenantB.id())),
                PipelineResponse.class)
            .getBody();
    assertThat(created).isNotNull();

    Map<String, Object> body =
        Map.of(
            "steps",
            List.of(
                Map.of(
                    "pipelet_id",
                    "plet-x",
                    "step_order",
                    1,
                    "connector_ids",
                    List.of(),
                    "service_ids",
                    List.of())));

    ResponseEntity<String> asA =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id() + "/steps",
            HttpMethod.PUT,
            new HttpEntity<>(body, jsonTenantHeaders(tenantA.id())),
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
