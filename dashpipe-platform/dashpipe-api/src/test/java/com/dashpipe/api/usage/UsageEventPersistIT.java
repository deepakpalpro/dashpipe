package com.dashpipe.api.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.dashpipe.api.tenant.CreateTenantRequest;
import com.dashpipe.api.tenant.TenantResponse;
import com.dashpipe.api.tenant.TenantStatus;
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

/** W5-US01: UsageEventEmitter → MySQL usage_events. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class UsageEventPersistIT {

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
  @Autowired private UsageEventEmitter usageEventEmitter;
  @Autowired private UsageEventRepository usageEventRepository;
  @Autowired private UsageEventService usageEventService;

  @Test
  void emitWebhookAccepted_persistsRows() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Usage " + suffix, "usage-" + suffix);
    String connectorId = "conn-" + suffix;

    long before = usageEventRepository.countByTenantId(tenant.id());
    usageEventEmitter.emitWebhookAccepted(tenant.id(), connectorId, 42L);

    List<UsageEventEntity> rows =
        usageEventRepository.findByTenantIdOrderByRecordedAtDesc(tenant.id());
    assertThat(rows.size() - before).isGreaterThanOrEqualTo(2);
    assertThat(rows)
        .anySatisfy(
            e -> {
              assertThat(e.getDimension()).isEqualTo(UsageEvent.WEBHOOK_EVENTS);
              assertThat(e.getQuantity()).isEqualByComparingTo("1.000000");
              assertThat(e.getConnectorId()).isEqualTo(connectorId);
            })
        .anySatisfy(
            e -> {
              assertThat(e.getDimension()).isEqualTo(UsageEvent.BYTES_IN);
              assertThat(e.getQuantity()).isEqualByComparingTo("42.000000");
            });
  }

  @Test
  void persist_sameIdempotencyKey_doesNotDoubleBill() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    TenantResponse tenant = createTenant("Usage Idem " + suffix, "uidem-" + suffix);
    Instant when = Instant.parse("2026-07-09T15:00:00Z");
    String key = "test-idem-" + suffix;

    UsageEvent event =
        new UsageEvent(UsageEvent.WEBHOOK_EVENTS, 1.0, tenant.id(), "c1", when)
            .withIdempotencyKey(key);

    String first = usageEventService.persist(event);
    String second = usageEventService.persist(event);

    assertThat(first).isEqualTo(second);
    assertThat(
            usageEventRepository.findByTenantIdOrderByRecordedAtDesc(tenant.id()).stream()
                .filter(e -> key.equals(e.getIdempotencyKey()))
                .count())
        .isEqualTo(1);
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
