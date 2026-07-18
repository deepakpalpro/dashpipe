package com.dashflow.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.billing.CreditBalanceService;
import com.dashflow.api.billing.RunBlockedResponse;
import com.dashflow.api.tenant.CreateTenantRequest;
import com.dashflow.api.tenant.TenantContextFilter;
import com.dashflow.api.tenant.TenantResponse;
import com.dashflow.api.tenant.TenantStatus;
import com.dashflow.api.usage.UsageAggregateEntity;
import com.dashflow.api.usage.UsageAggregateRepository;
import com.dashflow.api.usage.UsageEvent;
import com.dashflow.api.usage.UsageHourBucket;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;
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

/** W5-US06: hard limit / zero credit → HTTP 402 before run starts. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class RunBlockedIT {

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
  @Autowired private CreditBalanceService creditBalanceService;
  @Autowired private UsageAggregateRepository aggregateRepository;
  @Autowired private PipelineExecutionRepository executionRepository;

  @Test
  void returns402_whenZeroCredit() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Block0 " + suffix, "blk0-" + suffix);
    creditBalanceService.setBalance(tenant.id(), BigDecimal.ZERO);

    PipelineResponse pipeline = createActiveThreeStepPipeline(tenant.id(), "zero-" + suffix);
    long before = executionRepository.count();

    ResponseEntity<RunBlockedResponse> blocked =
        restTemplate.exchange(
            "/api/v1/pipelines/" + pipeline.id() + "/run",
            HttpMethod.POST,
            new HttpEntity<>(jsonTenantHeaders(tenant.id())),
            RunBlockedResponse.class);

    assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
    assertThat(blocked.getBody()).isNotNull();
    assertThat(blocked.getBody().code()).isEqualTo("NO_CREDIT");
    assertThat(blocked.getBody().error()).isEqualTo("payment_required");
    assertThat(executionRepository.count()).isEqualTo(before);
  }

  @Test
  void returns402_whenHardLimitBreached() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("BlockH " + suffix, "blkh-" + suffix);
    creditBalanceService.setBalance(tenant.id(), new BigDecimal("50.0000"));
    creditBalanceService.setQuotaConfig(
        tenant.id(),
        """
        {"dimensions":{"platform.pipeline_runs":{"soft":1,"hard":2}}}
        """);

    Instant hourStart = UsageHourBucket.truncateToHour(Instant.now());
    UsageAggregateEntity agg = new UsageAggregateEntity();
    agg.setId(UUID.randomUUID().toString());
    agg.setTenantId(tenant.id());
    agg.setPeriodStart(hourStart);
    agg.setPeriodEnd(UsageHourBucket.hourEnd(hourStart));
    agg.setGranularity(UsageAggregateEntity.GRANULARITY_HOURLY);
    agg.setDimension(UsageEvent.PIPELINE_RUNS);
    agg.setTotalQuantity(new BigDecimal("2.000000"));
    agg.setTotalCost(new BigDecimal("0.0200"));
    Instant now = Instant.now();
    agg.setCreatedAt(now);
    agg.setUpdatedAt(now);
    aggregateRepository.save(agg);

    PipelineResponse pipeline = createActiveThreeStepPipeline(tenant.id(), "hard-" + suffix);
    long before = executionRepository.count();

    ResponseEntity<RunBlockedResponse> blocked =
        restTemplate.exchange(
            "/api/v1/pipelines/" + pipeline.id() + "/run",
            HttpMethod.POST,
            new HttpEntity<>(jsonTenantHeaders(tenant.id())),
            RunBlockedResponse.class);

    assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
    assertThat(blocked.getBody()).isNotNull();
    assertThat(blocked.getBody().code()).isEqualTo("HARD_BLOCK");
    assertThat(blocked.getBody().breachedDimension()).isEqualTo(UsageEvent.PIPELINE_RUNS);
    assertThat(executionRepository.count()).isEqualTo(before);
  }

  @Test
  void returns202_whenAllowed() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Allow " + suffix, "allw-" + suffix);
    // default create credit is 100

    PipelineResponse pipeline = createActiveThreeStepPipeline(tenant.id(), "ok-" + suffix);

    ResponseEntity<PipelineRunResponse> accepted =
        restTemplate.exchange(
            "/api/v1/pipelines/" + pipeline.id() + "/run",
            HttpMethod.POST,
            new HttpEntity<>(jsonTenantHeaders(tenant.id())),
            PipelineRunResponse.class);

    assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(accepted.getBody()).isNotNull();
    assertThat(accepted.getBody().executionId()).isNotBlank();
  }

  private PipelineResponse createActiveThreeStepPipeline(String tenantId, String name) {
    PipelineResponse created =
        restTemplate
            .exchange(
                "/api/v1/pipelines",
                HttpMethod.POST,
                new HttpEntity<>(
                    new CreatePipelineRequest(name, null, null, null, null, null), jsonTenantHeaders(tenantId)),
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

  private static HttpHeaders jsonTenantHeaders(String tenantId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(TenantContextFilter.TENANT_ID_HEADER, tenantId);
    return headers;
  }
}
