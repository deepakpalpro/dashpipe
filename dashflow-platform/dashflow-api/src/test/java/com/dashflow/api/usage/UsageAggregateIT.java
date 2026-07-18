package com.dashflow.api.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.tenant.CreateTenantRequest;
import com.dashflow.api.tenant.TenantResponse;
import com.dashflow.api.tenant.TenantStatus;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/** W5-US03: seed usage_events → hourly usage_aggregates (idempotent). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class UsageAggregateIT {

  @BeforeAll
  static void requireComposeDeps() {
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
  @Autowired private UsageEventService usageEventService;
  @Autowired private UsageAggregateService usageAggregateService;
  @Autowired private UsageAggregateRepository aggregateRepository;

  @Test
  void aggregateHour_sumsEventsAndIsIdempotent() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Agg " + suffix, "agg-" + suffix);
    Instant periodStart = Instant.parse("2026-07-09T14:00:00Z");
    Instant inHour = Instant.parse("2026-07-09T14:22:00Z");

    usageEventService.persist(
        new UsageEvent(UsageEvent.PIPELINE_RUNS, 1.0, tenant.id(), null, inHour)
            .withIdempotencyKey("agg-run-a-" + suffix));
    usageEventService.persist(
        new UsageEvent(UsageEvent.PIPELINE_RUNS, 1.0, tenant.id(), null, inHour.plusSeconds(60))
            .withIdempotencyKey("agg-run-b-" + suffix));
    usageEventService.persist(
        new UsageEvent(UsageEvent.RECORDS_PROCESSED, 50.0, tenant.id(), null, inHour)
            .withIdempotencyKey("agg-rec-" + suffix));
    // Outside hour — must not be included
    usageEventService.persist(
        new UsageEvent(
                UsageEvent.PIPELINE_RUNS, 99.0, tenant.id(), null, Instant.parse("2026-07-09T15:01:00Z"))
            .withIdempotencyKey("agg-out-" + suffix));

    assertThat(usageAggregateService.aggregateHour(periodStart)).isGreaterThanOrEqualTo(2);

    List<UsageAggregateEntity> rows =
        aggregateRepository.findByTenantIdAndGranularityAndPeriodStart(
            tenant.id(), UsageAggregateEntity.GRANULARITY_HOURLY, periodStart);
    assertThat(rows).hasSize(2);

    UsageAggregateEntity runs =
        rows.stream()
            .filter(r -> UsageEvent.PIPELINE_RUNS.equals(r.getDimension()))
            .findFirst()
            .orElseThrow();
    assertThat(runs.getTotalQuantity()).isEqualByComparingTo(new BigDecimal("2.000000"));
    assertThat(runs.getTotalCost()).isEqualByComparingTo(new BigDecimal("0.0200"));
    assertThat(runs.getPeriodEnd()).isEqualTo(Instant.parse("2026-07-09T15:00:00Z"));

    String runId = runs.getId();
    assertThat(usageAggregateService.aggregateHour(periodStart)).isGreaterThanOrEqualTo(2);

    UsageAggregateEntity rerun =
        aggregateRepository
            .findByTenantIdAndDimensionAndGranularityAndPeriodStart(
                tenant.id(),
                UsageEvent.PIPELINE_RUNS,
                UsageAggregateEntity.GRANULARITY_HOURLY,
                periodStart)
            .orElseThrow();
    assertThat(rerun.getId()).isEqualTo(runId);
    assertThat(rerun.getTotalQuantity()).isEqualByComparingTo("2.000000");
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
}
