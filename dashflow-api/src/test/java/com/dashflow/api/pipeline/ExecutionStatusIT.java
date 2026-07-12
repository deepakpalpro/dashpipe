package com.dashflow.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.tenant.CreateTenantRequest;
import com.dashflow.api.tenant.TenantContextFilter;
import com.dashflow.api.tenant.TenantResponse;
import com.dashflow.api.tenant.TenantStatus;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/** W2-US07: list/get execution status after run; cross-tenant isolation. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ExecutionStatusIT {

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

  @Test
  void listAndGet_afterRun() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Exec Status " + suffix, "estatus-" + suffix);
    String tenantId = tenant.id();

    PipelineResponse pipeline = createActiveThreeStagePipeline(tenantId, "estatus-" + suffix);

    ResponseEntity<PipelineRunResponse> run =
        restTemplate.exchange(
            "/api/v1/pipelines/" + pipeline.id() + "/run",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenantId)),
            PipelineRunResponse.class);

    assertThat(run.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(run.getBody()).isNotNull();
    String executionId = run.getBody().executionId();

    await()
        .atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              ResponseEntity<PipelineExecutionResponse> detail =
                  restTemplate.exchange(
                      "/api/v1/pipelines/" + pipeline.id() + "/executions/" + executionId,
                      HttpMethod.GET,
                      new HttpEntity<>(tenantHeaders(tenantId)),
                      PipelineExecutionResponse.class);
              assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
              assertThat(detail.getBody()).isNotNull();
              assertThat(detail.getBody().id()).isEqualTo(executionId);
              assertThat(detail.getBody().pipelineId()).isEqualTo(pipeline.id());
              assertThat(detail.getBody().status()).isEqualTo(ExecutionStatus.COMPLETED);
              assertThat(detail.getBody().startedAt()).isNotNull();
            });

    ResponseEntity<List<PipelineExecutionResponse>> listed =
        restTemplate.exchange(
            "/api/v1/pipelines/" + pipeline.id() + "/executions",
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenantId)),
            new ParameterizedTypeReference<>() {});

    assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listed.getBody()).isNotNull();
    assertThat(listed.getBody())
        .extracting(PipelineExecutionResponse::id)
        .contains(executionId);
    assertThat(listed.getBody().getFirst().status()).isEqualTo(ExecutionStatus.COMPLETED);
  }

  @Test
  void get_asOtherTenant_404() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("Exec Iso A " + suffix, "eiso-a-" + suffix);
    TenantResponse tenantB = createTenant("Exec Iso B " + suffix, "eiso-b-" + suffix);

    PipelineResponse pipeline = createActiveThreeStagePipeline(tenantB.id(), "eiso-" + suffix);

    ResponseEntity<PipelineRunResponse> run =
        restTemplate.exchange(
            "/api/v1/pipelines/" + pipeline.id() + "/run",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenantB.id())),
            PipelineRunResponse.class);
    assertThat(run.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(run.getBody()).isNotNull();
    String executionId = run.getBody().executionId();

    await()
        .atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              ResponseEntity<PipelineExecutionResponse> detail =
                  restTemplate.exchange(
                      "/api/v1/pipelines/" + pipeline.id() + "/executions/" + executionId,
                      HttpMethod.GET,
                      new HttpEntity<>(tenantHeaders(tenantB.id())),
                      PipelineExecutionResponse.class);
              assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
              assertThat(detail.getBody()).isNotNull();
              assertThat(detail.getBody().status()).isEqualTo(ExecutionStatus.COMPLETED);
            });

    ResponseEntity<String> listAsA =
        restTemplate.exchange(
            "/api/v1/pipelines/" + pipeline.id() + "/executions",
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenantA.id())),
            String.class);
    assertThat(listAsA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<String> getAsA =
        restTemplate.exchange(
            "/api/v1/pipelines/" + pipeline.id() + "/executions/" + executionId,
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenantA.id())),
            String.class);
    assertThat(getAsA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private PipelineResponse createActiveThreeStagePipeline(String tenantId, String namePrefix) {
    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest(namePrefix, null, null, null, null, null),
                    jsonTenantHeaders(tenantId)),
                PipelineResponse.class)
            .getBody();
    assertThat(created).isNotNull();

    Map<String, Object> stepsBody =
        Map.of(
            "steps",
            List.of(
                Map.of(
                    "pipelet_id",
                    "plet-source",
                    "step_order",
                    1,
                    "connector_ids",
                    List.of(),
                    "service_ids",
                    List.of()),
                Map.of(
                    "pipelet_id",
                    "plet-proc",
                    "step_order",
                    2,
                    "connector_ids",
                    List.of(),
                    "service_ids",
                    List.of()),
                Map.of(
                    "pipelet_id",
                    "plet-dest",
                    "step_order",
                    3,
                    "connector_ids",
                    List.of(),
                    "service_ids",
                    List.of())));

    restTemplate.exchange(
        "/api/v1/pipelines/" + created.id() + "/steps",
        HttpMethod.PUT,
        new HttpEntity<>(stepsBody, jsonTenantHeaders(tenantId)),
        PipelineResponse.class);

    UpdatePipelineRequest activate =
        new UpdatePipelineRequest(
            created.name(), created.description(), null, null, PipelineStatus.ACTIVE, null, null);
    restTemplate.exchange(
        "/api/v1/pipelines/" + created.id(),
        HttpMethod.PUT,
        new HttpEntity<>(activate, jsonTenantHeaders(tenantId)),
        PipelineResponse.class);

    return created;
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
