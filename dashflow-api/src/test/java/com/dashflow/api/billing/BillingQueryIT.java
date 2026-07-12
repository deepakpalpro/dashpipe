package com.dashflow.api.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.tenant.CreateTenantRequest;
import com.dashflow.api.tenant.TenantContextFilter;
import com.dashflow.api.tenant.TenantResponse;
import com.dashflow.api.tenant.TenantStatus;
import com.dashflow.api.usage.UsageAggregateService;
import com.dashflow.api.usage.UsageEvent;
import com.dashflow.api.usage.UsageEventService;
import com.dashflow.api.usage.UsageHourBucket;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;
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

/** W5-US05: §3.5 usage/billing/quota APIs + tenant isolation. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class BillingQueryIT {

  /** Absolute quantity tolerance for fixture summary (KB). */
  private static final BigDecimal QTY_TOLERANCE = new BigDecimal("0.000001");

  /** Absolute cost tolerance (± $0.01 or 1% — use absolute for small stubs). */
  private static final BigDecimal COST_TOLERANCE = new BigDecimal("0.01");

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
  @Autowired private CreditBalanceService creditBalanceService;

  @Test
  void usageSummary_matchesFixtureWithinTolerance() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Bill " + suffix, "bill-" + suffix);
    creditBalanceService.setBalance(tenant.id(), new BigDecimal("100.0000"));

    Instant inHour = Instant.now().minusSeconds(120);
    Instant hourStart = UsageHourBucket.truncateToHour(inHour);

    usageEventService.persist(
        new UsageEvent(UsageEvent.PIPELINE_RUNS, 2.0, tenant.id(), null, inHour)
            .withIdempotencyKey("bq-run-" + suffix));
    usageEventService.persist(
        new UsageEvent(UsageEvent.RECORDS_PROCESSED, 100.0, tenant.id(), null, inHour)
            .withIdempotencyKey("bq-rec-" + suffix));
    usageAggregateService.aggregateHour(hourStart);

    ResponseEntity<BillingDtos.UsageSummaryResponse> response =
        restTemplate.exchange(
            "/api/v1/tenants/" + tenant.id() + "/usage?period=current",
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            BillingDtos.UsageSummaryResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    BillingDtos.UsageSummaryResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tenantId()).isEqualTo(tenant.id());
    assertThat(body.creditBalance()).isEqualByComparingTo("99.9790");
    // 2*0.01 + 100*0.00001 = 0.0210 deducted from 100

    Map<String, BillingDtos.DimensionUsage> dims = body.dimensions();
    assertThat(dims).containsKeys(UsageEvent.PIPELINE_RUNS, UsageEvent.RECORDS_PROCESSED);

    BillingDtos.DimensionUsage runs = dims.get(UsageEvent.PIPELINE_RUNS);
    assertThat(runs.quantity()).isCloseTo(new BigDecimal("2.000000"), within(QTY_TOLERANCE));
    assertThat(runs.cost()).isCloseTo(new BigDecimal("0.0200"), within(COST_TOLERANCE));

    BillingDtos.DimensionUsage records = dims.get(UsageEvent.RECORDS_PROCESSED);
    assertThat(records.quantity()).isCloseTo(new BigDecimal("100.000000"), within(QTY_TOLERANCE));
    assertThat(records.cost()).isCloseTo(new BigDecimal("0.0010"), within(COST_TOLERANCE));

    assertThat(body.totalCost()).isCloseTo(new BigDecimal("0.0210"), within(COST_TOLERANCE));
  }

  @Test
  void crossTenant_returns404() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenantA = createTenant("A " + suffix, "ba-" + suffix);
    TenantResponse tenantB = createTenant("B " + suffix, "bb-" + suffix);
    creditBalanceService.setBalance(tenantA.id(), new BigDecimal("10.0000"));

    ResponseEntity<String> asB =
        restTemplate.exchange(
            "/api/v1/tenants/" + tenantA.id() + "/usage",
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenantB.id())),
            String.class);

    assertThat(asB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void usageEvents_paginated_and_quota_and_periods() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Ev " + suffix, "bev-" + suffix);
    creditBalanceService.setBalance(tenant.id(), new BigDecimal("50.0000"));

    Instant when = Instant.now().minusSeconds(60);
    usageEventService.persist(
        new UsageEvent(UsageEvent.WEBHOOK_EVENTS, 1.0, tenant.id(), "c1", when)
            .withIdempotencyKey("bq-wh-" + suffix));

    ResponseEntity<BillingDtos.UsageEventsPageResponse> events =
        restTemplate.exchange(
            "/api/v1/tenants/" + tenant.id() + "/usage/events?page=0&size=10",
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            BillingDtos.UsageEventsPageResponse.class);

    assertThat(events.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(events.getBody()).isNotNull();
    assertThat(events.getBody().items()).isNotEmpty();
    assertThat(events.getBody().items())
        .anySatisfy(i -> assertThat(i.dimension()).isEqualTo(UsageEvent.WEBHOOK_EVENTS));

    ResponseEntity<BillingDtos.QuotaStatusResponse> quota =
        restTemplate.exchange(
            "/api/v1/tenants/" + tenant.id() + "/quota",
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            BillingDtos.QuotaStatusResponse.class);
    assertThat(quota.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(quota.getBody()).isNotNull();
    assertThat(quota.getBody().decision()).isEqualTo("ALLOW");
    assertThat(quota.getBody().allowed()).isTrue();

    ResponseEntity<BillingDtos.BillingPeriodsResponse> periods =
        restTemplate.exchange(
            "/api/v1/tenants/" + tenant.id() + "/billing/periods",
            HttpMethod.GET,
            new HttpEntity<>(tenantHeaders(tenant.id())),
            BillingDtos.BillingPeriodsResponse.class);
    assertThat(periods.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(periods.getBody()).isNotNull();
    assertThat(periods.getBody().periods()).hasSize(1);
    assertThat(periods.getBody().periods().getFirst().status()).isEqualTo("open");
  }

  private static org.assertj.core.data.Offset<BigDecimal> within(BigDecimal tol) {
    return org.assertj.core.data.Offset.offset(tol);
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
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(TenantContextFilter.TENANT_ID_HEADER, tenantId);
    return headers;
  }
}
