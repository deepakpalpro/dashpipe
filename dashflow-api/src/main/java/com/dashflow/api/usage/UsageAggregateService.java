package com.dashflow.api.usage;

import com.dashflow.api.billing.CreditBalanceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rolls raw {@code usage_events} into hourly {@code usage_aggregates}. Safe to re-run for the same
 * hour (upsert by tenant + dimension + period). Deducts credit by cost delta on write.
 */
@Service
public class UsageAggregateService {

  private static final Logger log = LoggerFactory.getLogger(UsageAggregateService.class);

  private final UsageEventRepository eventRepository;
  private final UsageAggregateRepository aggregateRepository;
  private final CreditBalanceService creditBalanceService;
  private final Clock clock;

  public UsageAggregateService(
      UsageEventRepository eventRepository,
      UsageAggregateRepository aggregateRepository,
      CreditBalanceService creditBalanceService,
      Clock clock) {
    this.eventRepository = eventRepository;
    this.aggregateRepository = aggregateRepository;
    this.creditBalanceService = creditBalanceService;
    this.clock = clock;
  }

  /** Aggregate the previous complete UTC hour (architecture cron intent). */
  @Transactional
  public int aggregatePreviousHour() {
    Instant periodStart = UsageHourBucket.previousHourStart(clock.instant());
    return aggregateHour(periodStart);
  }

  /**
   * Aggregate events with {@code recorded_at} in {@code [periodStart, periodStart+1h)}. Upserts
   * existing rows for the same tenant/dimension/hour.
   *
   * @return number of aggregate rows written (insert or update)
   */
  @Transactional
  public int aggregateHour(Instant periodStart) {
    Instant start = UsageHourBucket.truncateToHour(periodStart);
    Instant end = UsageHourBucket.hourEnd(start);
    Instant now = clock.instant();

    List<UsageDimensionSum> sums =
        eventRepository.sumByTenantAndDimensionForPeriod(start, end);

    int written = 0;
    for (UsageDimensionSum sum : sums) {
      BigDecimal quantity =
          sum.getTotalQuantity() == null
              ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
              : sum.getTotalQuantity().setScale(6, RoundingMode.HALF_UP);
      BigDecimal cost = UsageUnitPrices.costFor(sum.getDimension(), quantity);

      var existing =
          aggregateRepository.findByTenantIdAndDimensionAndGranularityAndPeriodStart(
              sum.getTenantId(),
              sum.getDimension(),
              UsageAggregateEntity.GRANULARITY_HOURLY,
              start);

      if (existing.isPresent()) {
        UsageAggregateEntity row = existing.get();
        BigDecimal previousCost =
            row.getTotalCost() == null ? BigDecimal.ZERO : row.getTotalCost();
        BigDecimal delta = cost.subtract(previousCost);
        row.setTotalQuantity(quantity);
        row.setTotalCost(cost);
        row.setUpdatedAt(now);
        aggregateRepository.save(row);
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
          creditBalanceService.deduct(sum.getTenantId(), delta);
        }
      } else {
        UsageAggregateEntity row = new UsageAggregateEntity();
        row.setId(UUID.randomUUID().toString());
        row.setTenantId(sum.getTenantId());
        row.setPeriodStart(start);
        row.setPeriodEnd(end);
        row.setGranularity(UsageAggregateEntity.GRANULARITY_HOURLY);
        row.setDimension(sum.getDimension());
        row.setTotalQuantity(quantity);
        row.setTotalCost(cost);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        aggregateRepository.save(row);
        if (cost.compareTo(BigDecimal.ZERO) != 0) {
          creditBalanceService.deduct(sum.getTenantId(), cost);
        }
      }
      written++;
    }

    log.info(
        "usage hourly aggregate periodStart={} periodEnd={} rows={}", start, end, written);
    return written;
  }
}
