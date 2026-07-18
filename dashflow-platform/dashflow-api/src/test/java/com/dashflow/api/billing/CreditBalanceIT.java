package com.dashflow.api.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashflow.api.tenant.CreateTenantRequest;
import com.dashflow.api.tenant.TenantResponse;
import com.dashflow.api.tenant.TenantStatus;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/** W5-US04: credit balance deduct persists. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class CreditBalanceIT {

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
  @Autowired private CreditBalanceService creditBalanceService;
  @Autowired private QuotaService quotaService;

  @Test
  void deduct_and_evaluate_noCredit() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Credit " + suffix, "cred-" + suffix);

    creditBalanceService.setBalance(tenant.id(), new BigDecimal("1.0000"));
    assertThat(creditBalanceService.currentBalance(tenant.id())).isEqualByComparingTo("1.0000");

    creditBalanceService.deduct(tenant.id(), new BigDecimal("1.0000"));
    assertThat(creditBalanceService.currentBalance(tenant.id())).isEqualByComparingTo("0.0000");

    QuotaDecision decision = quotaService.evaluateTenant(tenant.id());
    assertThat(decision.code()).isEqualTo(QuotaDecisionCode.NO_CREDIT);
    assertThat(decision.blocksRun()).isTrue();
  }

  @Test
  void evaluate_softWarn_fromConfigAndUsage() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Quota " + suffix, "quota-" + suffix);
    creditBalanceService.setBalance(tenant.id(), new BigDecimal("10.0000"));

    // Direct evaluate with synthetic usage (no aggregates needed)
    QuotaDecision decision =
        quotaService.evaluate(
            new BigDecimal("10.0000"),
            """
            {"dimensions":{"platform.pipeline_runs":{"soft":5,"hard":50}}}
            """,
            java.util.Map.of("platform.pipeline_runs", new BigDecimal("10")));

    assertThat(decision.code()).isEqualTo(QuotaDecisionCode.SOFT_WARN);
    assertThat(decision.allowed()).isTrue();
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
