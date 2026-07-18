package com.dashpipe.api.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dashpipe.api.billing.CreditBalanceService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** W5-US03: hourly bucket math + idempotent re-run. */
@ExtendWith(MockitoExtension.class)
class UsageAggregateJobTest {

  @Mock private UsageEventRepository eventRepository;
  @Mock private UsageAggregateRepository aggregateRepository;
  @Mock private CreditBalanceService creditBalanceService;

  private Clock clock;
  private UsageAggregateService service;

  @BeforeEach
  void setUp() {
    // 15:30 UTC → previous complete hour is 14:00–15:00
    clock = Clock.fixed(Instant.parse("2026-07-09T15:30:00Z"), ZoneOffset.UTC);
    service =
        new UsageAggregateService(
            eventRepository, aggregateRepository, creditBalanceService, clock);
  }

  @Test
  void truncateToHour_isUtc() {
    assertThat(UsageHourBucket.truncateToHour(Instant.parse("2026-07-09T15:59:59.999Z")))
        .isEqualTo(Instant.parse("2026-07-09T15:00:00Z"));
    assertThat(UsageHourBucket.previousHourStart(Instant.parse("2026-07-09T15:00:00Z")))
        .isEqualTo(Instant.parse("2026-07-09T14:00:00Z"));
  }

  @Test
  void aggregatePreviousHour_createsRowsWithCost() {
    Instant start = Instant.parse("2026-07-09T14:00:00Z");
    Instant end = Instant.parse("2026-07-09T15:00:00Z");

    when(eventRepository.sumByTenantAndDimensionForPeriod(start, end))
        .thenReturn(
            List.of(
                sum("T001", UsageEvent.PIPELINE_RUNS, "2.000000"),
                sum("T001", UsageEvent.RECORDS_PROCESSED, "100.000000")));

    when(aggregateRepository.findByTenantIdAndDimensionAndGranularityAndPeriodStart(
            any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    when(aggregateRepository.save(any(UsageAggregateEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    assertThat(service.aggregatePreviousHour()).isEqualTo(2);

    ArgumentCaptor<UsageAggregateEntity> captor =
        ArgumentCaptor.forClass(UsageAggregateEntity.class);
    verify(aggregateRepository, times(2)).save(captor.capture());

    UsageAggregateEntity runs =
        captor.getAllValues().stream()
            .filter(e -> UsageEvent.PIPELINE_RUNS.equals(e.getDimension()))
            .findFirst()
            .orElseThrow();
    assertThat(runs.getTenantId()).isEqualTo("T001");
    assertThat(runs.getPeriodStart()).isEqualTo(start);
    assertThat(runs.getPeriodEnd()).isEqualTo(end);
    assertThat(runs.getGranularity()).isEqualTo(UsageAggregateEntity.GRANULARITY_HOURLY);
    assertThat(runs.getTotalQuantity()).isEqualByComparingTo("2.000000");
    // 2 * $0.01
    assertThat(runs.getTotalCost()).isEqualByComparingTo("0.0200");

    UsageAggregateEntity records =
        captor.getAllValues().stream()
            .filter(e -> UsageEvent.RECORDS_PROCESSED.equals(e.getDimension()))
            .findFirst()
            .orElseThrow();
    // 100 * $0.00001
    assertThat(records.getTotalCost()).isEqualByComparingTo("0.0010");

    verify(creditBalanceService).deduct("T001", new BigDecimal("0.0200"));
    verify(creditBalanceService).deduct("T001", new BigDecimal("0.0010"));
  }

  @Test
  void aggregateHour_rerun_updatesExistingRow() {
    Instant start = Instant.parse("2026-07-09T14:00:00Z");
    Instant end = Instant.parse("2026-07-09T15:00:00Z");

    UsageAggregateEntity existing = new UsageAggregateEntity();
    existing.setId("agg-1");
    existing.setTenantId("T001");
    existing.setDimension(UsageEvent.PIPELINE_RUNS);
    existing.setGranularity(UsageAggregateEntity.GRANULARITY_HOURLY);
    existing.setPeriodStart(start);
    existing.setPeriodEnd(end);
    existing.setTotalQuantity(new BigDecimal("1.000000"));
    existing.setTotalCost(new BigDecimal("0.0100"));
    existing.setCreatedAt(Instant.parse("2026-07-09T15:01:00Z"));
    existing.setUpdatedAt(Instant.parse("2026-07-09T15:01:00Z"));

    when(eventRepository.sumByTenantAndDimensionForPeriod(start, end))
        .thenReturn(List.of(sum("T001", UsageEvent.PIPELINE_RUNS, "3.000000")));
    when(aggregateRepository.findByTenantIdAndDimensionAndGranularityAndPeriodStart(
            eq("T001"),
            eq(UsageEvent.PIPELINE_RUNS),
            eq(UsageAggregateEntity.GRANULARITY_HOURLY),
            eq(start)))
        .thenReturn(Optional.of(existing));
    when(aggregateRepository.save(any(UsageAggregateEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    assertThat(service.aggregateHour(start)).isEqualTo(1);

    ArgumentCaptor<UsageAggregateEntity> captor =
        ArgumentCaptor.forClass(UsageAggregateEntity.class);
    verify(aggregateRepository).save(captor.capture());
    UsageAggregateEntity updated = captor.getValue();
    assertThat(updated.getId()).isEqualTo("agg-1");
    assertThat(updated.getTotalQuantity()).isEqualByComparingTo("3.000000");
    // 3 * $0.01
    assertThat(updated.getTotalCost()).isEqualByComparingTo("0.0300");
    assertThat(updated.getUpdatedAt()).isEqualTo(clock.instant());
    assertThat(updated.getCreatedAt()).isEqualTo(Instant.parse("2026-07-09T15:01:00Z"));
    // cost delta 0.03 - 0.01
    verify(creditBalanceService).deduct("T001", new BigDecimal("0.0200"));
  }

  @Test
  void aggregateHour_noEvents_writesNothing() {
    Instant start = Instant.parse("2026-07-09T10:00:00Z");
    when(eventRepository.sumByTenantAndDimensionForPeriod(start, start.plusSeconds(3600)))
        .thenReturn(List.of());

    assertThat(service.aggregateHour(start)).isEqualTo(0);
    verify(aggregateRepository, never()).save(any());
  }

  @Test
  void job_delegatesToPreviousHour() {
    when(eventRepository.sumByTenantAndDimensionForPeriod(any(), any())).thenReturn(List.of());
    UsageAggregateJob job = new UsageAggregateJob(service);
    job.runHourly();
    verify(eventRepository)
        .sumByTenantAndDimensionForPeriod(
            Instant.parse("2026-07-09T14:00:00Z"), Instant.parse("2026-07-09T15:00:00Z"));
  }

  private static UsageDimensionSum sum(String tenantId, String dimension, String qty) {
    return new UsageDimensionSum() {
      @Override
      public String getTenantId() {
        return tenantId;
      }

      @Override
      public String getDimension() {
        return dimension;
      }

      @Override
      public BigDecimal getTotalQuantity() {
        return new BigDecimal(qty);
      }
    };
  }
}
