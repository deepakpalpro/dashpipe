package com.dashflow.api.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** W5-US01: persist mapping + idempotency. */
@ExtendWith(MockitoExtension.class)
class UsageEventServiceTest {

  @Mock private UsageEventRepository repository;

  private UsageEventService service;

  @BeforeEach
  void setUp() {
    service = new UsageEventService(repository);
  }

  @Test
  void persist_mapsFieldsAndSaves() {
    Instant when = Instant.parse("2026-07-09T12:00:00Z");
    UsageEvent event =
        new UsageEvent(UsageEvent.WEBHOOK_EVENTS, 1.0, "T001", "conn-1", when)
            .withIdempotencyKey("key-1");

    when(repository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
    when(repository.save(any(UsageEventEntity.class)))
        .thenAnswer(
            inv -> {
              UsageEventEntity e = inv.getArgument(0);
              assertThat(e.getId()).isNotBlank();
              return e;
            });

    String id = service.persist(event);
    assertThat(id).isNotBlank();

    ArgumentCaptor<UsageEventEntity> captor = ArgumentCaptor.forClass(UsageEventEntity.class);
    verify(repository).save(captor.capture());
    UsageEventEntity saved = captor.getValue();
    assertThat(saved.getTenantId()).isEqualTo("T001");
    assertThat(saved.getConnectorId()).isEqualTo("conn-1");
    assertThat(saved.getDimension()).isEqualTo(UsageEvent.WEBHOOK_EVENTS);
    assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("1.000000"));
    assertThat(saved.getRecordedAt()).isEqualTo(when);
    assertThat(saved.getIdempotencyKey()).isEqualTo("key-1");
  }

  @Test
  void persist_secondCallWithSameKey_doesNotSaveAgain() {
    UsageEventEntity existing = new UsageEventEntity();
    existing.setId("existing-id");
    existing.setIdempotencyKey("dup-key");
    when(repository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

    UsageEvent event =
        new UsageEvent(UsageEvent.BYTES_IN, 10.0, "T001", "c1", Instant.now())
            .withIdempotencyKey("dup-key");

    assertThat(service.persist(event)).isEqualTo("existing-id");
    verify(repository, never()).save(any());
  }

  @Test
  void deriveIdempotencyKey_isStable() {
    Instant when = Instant.parse("2026-07-09T00:00:00Z");
    UsageEvent a = new UsageEvent(UsageEvent.BYTES_IN, 5.0, "T001", "c1", when);
    UsageEvent b = new UsageEvent(UsageEvent.BYTES_IN, 5.0, "T001", "c1", when);
    assertThat(UsageEventService.deriveIdempotencyKey(a))
        .isEqualTo(UsageEventService.deriveIdempotencyKey(b));
  }
}
