package com.dashflow.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.k8s.StubPipeletJobClient;
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

/** W2-US04/US05: async run returns execution id; stub Jobs + worker complete stages. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class PipelineRunIT {

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

  @BeforeEach
  void clearJobCreates() {
    stubPipeletJobClient.clear();
  }

  @Test
  void run_returnsExecutionId() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Run " + suffix, "run-" + suffix);
    String tenantId = tenant.id();

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest("async-run-" + suffix, null, null, null, null, null),
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

    ResponseEntity<PipelineRunResponse> run =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id() + "/run",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenantId)),
            PipelineRunResponse.class);

    assertThat(run.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(run.getBody()).isNotNull();
    assertThat(run.getBody().executionId()).isNotBlank();
    assertThat(run.getBody().pipelineId()).isEqualTo(created.id());
    assertThat(run.getBody().status())
        .isIn(ExecutionStatus.PENDING, ExecutionStatus.RUNNING, ExecutionStatus.COMPLETED);

    String executionId = run.getBody().executionId();
    await()
        .atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              ResponseEntity<PipelineExecutionResponse> fetched =
                  restTemplate.exchange(
                      "/api/v1/pipelines/" + created.id() + "/executions/" + executionId,
                      HttpMethod.GET,
                      new HttpEntity<>(tenantHeaders(tenantId)),
                      PipelineExecutionResponse.class);
              assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
              assertThat(fetched.getBody()).isNotNull();
              assertThat(fetched.getBody().status()).isEqualTo(ExecutionStatus.COMPLETED);
            });

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(stubPipeletJobClient.getCreated())
                    .hasSize(3)
                    .allSatisfy(
                        req -> {
                          assertThat(req.tenantId()).isEqualTo(tenantId);
                          assertThat(req.pipelineId()).isEqualTo(created.id());
                          assertThat(req.executionId()).isEqualTo(executionId);
                          assertThat(req.namespace())
                              .isEqualTo(
                                  com.dashflow.api.k8s.PipeletK8sProperties
                                      .namespaceForTenant(tenantId));
                        }));
    assertThat(stubPipeletJobClient.getCreated())
        .extracting(req -> req.stageOrder())
        .containsExactly(1, 2, 3);
    assertThat(stubPipeletJobClient.getCreated())
        .extracting(req -> req.pipeletId())
        .containsExactly("plet-source", "plet-proc", "plet-dest");
    assertThat(stubPipeletJobClient.getCreated())
        .allSatisfy(
            req -> {
              assertThat(req.ioMode()).isEqualTo(PipelineIoMode.QUEUE);
              assertThat(req.amqpUrl()).isNotBlank();
              assertThat(req.inputQueue()).isNotBlank();
            });
  }

  @Test
  void run_withStdioIoMode_passesModeOnJobRequests() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Stdio Run " + suffix, "stdio-" + suffix);
    String tenantId = tenant.id();

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest(
                        "stdio-run-" + suffix,
                        null,
                        null,
                        null,
                        null,
                        objectMapper().createObjectNode().put("ioMode", "stdio")),
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
                    "plet-a",
                    "step_order",
                    1,
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

    ResponseEntity<PipelineRunResponse> run =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id() + "/run",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenantId)),
            PipelineRunResponse.class);
    assertThat(run.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(stubPipeletJobClient.getCreated())
                    .isNotEmpty()
                    .allSatisfy(req -> assertThat(req.ioMode()).isEqualTo(PipelineIoMode.STDIO)));
  }

  private static com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
    return new com.fasterxml.jackson.databind.ObjectMapper();
  }

  @Test
  void run_asOtherTenant_returnsNotFound() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("Run Iso A " + suffix, "riso-a-" + suffix);
    TenantResponse tenantB = createTenant("Run Iso B " + suffix, "riso-b-" + suffix);

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest("owned-b-" + suffix, null, null, null, null, null),
                    jsonTenantHeaders(tenantB.id())),
                PipelineResponse.class)
            .getBody();
    assertThat(created).isNotNull();

    ResponseEntity<String> asA =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id() + "/run",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenantA.id())),
            String.class);

    assertThat(asA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void run_draftPipeline_returnsBadRequest() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Draft Run " + suffix, "drun-" + suffix);

    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest("still-draft-" + suffix, null, null, null, null, null),
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
                    "plet-x",
                    "step_order",
                    1,
                    "connector_ids",
                    List.of(),
                    "service_ids",
                    List.of())));
    restTemplate.exchange(
        "/api/v1/pipelines/" + created.id() + "/steps",
        HttpMethod.PUT,
        new HttpEntity<>(stepsBody, jsonTenantHeaders(tenant.id())),
        PipelineResponse.class);

    ResponseEntity<String> run =
        restTemplate.exchange(
            "/api/v1/pipelines/" + created.id() + "/run",
            HttpMethod.POST,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            String.class);

    assertThat(run.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
