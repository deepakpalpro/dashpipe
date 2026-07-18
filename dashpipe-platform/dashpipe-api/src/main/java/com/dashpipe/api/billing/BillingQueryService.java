package com.dashpipe.api.billing;

import com.dashpipe.api.tenant.Tenant;
import com.dashpipe.api.tenant.TenantContext;
import com.dashpipe.api.tenant.TenantContextRequiredException;
import com.dashpipe.api.tenant.TenantNotFoundException;
import com.dashpipe.api.tenant.TenantRepository;
import com.dashpipe.api.usage.UsageAggregateEntity;
import com.dashpipe.api.usage.UsageAggregateRepository;
import com.dashpipe.api.usage.UsageEventEntity;
import com.dashpipe.api.usage.UsageEventRepository;
import com.dashpipe.api.usage.UsageUnitPrices;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assembles §3.5 usage / billing / quota responses with tenant isolation. */
@Service
public class BillingQueryService {

  private final TenantRepository tenantRepository;
  private final UsageAggregateRepository aggregateRepository;
  private final UsageEventRepository eventRepository;
  private final QuotaConfigParser configParser;
  private final QuotaEvaluator evaluator;
  private final Clock clock;

  public BillingQueryService(
      TenantRepository tenantRepository,
      UsageAggregateRepository aggregateRepository,
      UsageEventRepository eventRepository,
      QuotaConfigParser configParser,
      QuotaEvaluator evaluator,
      Clock clock) {
    this.tenantRepository = tenantRepository;
    this.aggregateRepository = aggregateRepository;
    this.eventRepository = eventRepository;
    this.configParser = configParser;
    this.evaluator = evaluator;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public BillingDtos.UsageSummaryResponse usageSummary(String pathTenantId) {
    Tenant tenant = requireAccessibleTenant(pathTenantId);
    Instant now = clock.instant();
    Instant periodStart = utcMonthStart(now);
    Instant periodEndExclusive = utcNextMonthStart(now);
    Instant periodEndInclusive = periodEndExclusive.minusMillis(1);

    List<UsageAggregateEntity> rows =
        aggregateRepository
            .findByTenantIdAndGranularityAndPeriodStartGreaterThanEqualAndPeriodStartLessThan(
                tenant.getId(),
                UsageAggregateEntity.GRANULARITY_HOURLY,
                periodStart,
                periodEndExclusive);

    Map<String, BillingDtos.DimensionUsage> dimensions = new TreeMap<>();
    BigDecimal totalCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    Map<String, BigDecimal> qtyByDim = new LinkedHashMap<>();
    Map<String, BigDecimal> costByDim = new LinkedHashMap<>();
    for (UsageAggregateEntity row : rows) {
      qtyByDim.merge(
          row.getDimension(),
          row.getTotalQuantity() == null ? BigDecimal.ZERO : row.getTotalQuantity(),
          BigDecimal::add);
      costByDim.merge(
          row.getDimension(),
          row.getTotalCost() == null ? BigDecimal.ZERO : row.getTotalCost(),
          BigDecimal::add);
    }
    for (Map.Entry<String, BigDecimal> e : qtyByDim.entrySet()) {
      BigDecimal qty = e.getValue().setScale(6, RoundingMode.HALF_UP);
      BigDecimal cost =
          costByDim
              .getOrDefault(e.getKey(), UsageUnitPrices.costFor(e.getKey(), qty))
              .setScale(4, RoundingMode.HALF_UP);
      dimensions.put(e.getKey(), new BillingDtos.DimensionUsage(qty, cost));
      totalCost = totalCost.add(cost);
    }

    BigDecimal credit =
        tenant.getCreditBalance() == null ? BigDecimal.ZERO : tenant.getCreditBalance();
    return new BillingDtos.UsageSummaryResponse(
        tenant.getId(),
        periodStart,
        periodEndInclusive,
        dimensions,
        totalCost.setScale(4, RoundingMode.HALF_UP),
        credit);
  }

  @Transactional(readOnly = true)
  public BillingDtos.UsageEventsPageResponse usageEvents(String pathTenantId, int page, int size) {
    Tenant tenant = requireAccessibleTenant(pathTenantId);
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    Page<UsageEventEntity> result =
        eventRepository.findByTenantIdOrderByRecordedAtDesc(
            tenant.getId(), PageRequest.of(safePage, safeSize));

    List<BillingDtos.UsageEventItem> items =
        result.getContent().stream()
            .map(
                e ->
                    new BillingDtos.UsageEventItem(
                        e.getId(),
                        e.getDimension(),
                        e.getQuantity(),
                        e.getUnit(),
                        e.getExecutionId(),
                        e.getPipelineId(),
                        e.getPipeletId(),
                        e.getConnectorId(),
                        e.getRecordedAt(),
                        e.getIdempotencyKey()))
            .toList();

    return new BillingDtos.UsageEventsPageResponse(
        tenant.getId(),
        items,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public BillingDtos.QuotaStatusResponse quotaStatus(String pathTenantId) {
    Tenant tenant = requireAccessibleTenant(pathTenantId);
    Instant now = clock.instant();
    Instant periodStart = utcMonthStart(now);
    Instant periodEndExclusive = utcNextMonthStart(now);

    Map<String, BigDecimal> usage = new LinkedHashMap<>();
    List<UsageAggregateEntity> rows =
        aggregateRepository
            .findByTenantIdAndGranularityAndPeriodStartGreaterThanEqualAndPeriodStartLessThan(
                tenant.getId(),
                UsageAggregateEntity.GRANULARITY_HOURLY,
                periodStart,
                periodEndExclusive);
    for (UsageAggregateEntity row : rows) {
      usage.merge(
          row.getDimension(),
          row.getTotalQuantity() == null ? BigDecimal.ZERO : row.getTotalQuantity(),
          BigDecimal::add);
    }

    QuotaConfig config = configParser.parse(tenant.getQuotaConfig());
    QuotaDecision decision = evaluator.evaluate(tenant.getCreditBalance(), config, usage);

    Map<String, BillingDtos.DimensionQuotaStatus> dimStatus = new TreeMap<>();
    for (Map.Entry<String, QuotaConfig.DimensionLimit> entry : config.dimensions().entrySet()) {
      dimStatus.put(
          entry.getKey(),
          new BillingDtos.DimensionQuotaStatus(
              entry.getValue().soft(),
              entry.getValue().hard(),
              usage.getOrDefault(entry.getKey(), BigDecimal.ZERO)));
    }

    return new BillingDtos.QuotaStatusResponse(
        tenant.getId(),
        decision.code().name(),
        decision.message(),
        decision.allowed(),
        decision.creditBalance(),
        decision.breachedDimension(),
        decision.softLimit(),
        decision.hardLimit(),
        decision.currentUsage(),
        dimStatus);
  }

  /**
   * Stub billing periods: current UTC calendar month from hourly aggregate costs (Wave 5 — no
   * invoice table yet).
   */
  @Transactional(readOnly = true)
  public BillingDtos.BillingPeriodsResponse billingPeriods(String pathTenantId) {
    Tenant tenant = requireAccessibleTenant(pathTenantId);
    Instant now = clock.instant();
    Instant periodStart = utcMonthStart(now);
    Instant periodEndExclusive = utcNextMonthStart(now);

    List<UsageAggregateEntity> rows =
        aggregateRepository
            .findByTenantIdAndGranularityAndPeriodStartGreaterThanEqualAndPeriodStartLessThan(
                tenant.getId(),
                UsageAggregateEntity.GRANULARITY_HOURLY,
                periodStart,
                periodEndExclusive);

    BigDecimal total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    for (UsageAggregateEntity row : rows) {
      if (row.getTotalCost() != null) {
        total = total.add(row.getTotalCost());
      }
    }

    YearMonth ym = YearMonth.from(now.atZone(ZoneOffset.UTC));
    BillingDtos.BillingPeriodItem current =
        new BillingDtos.BillingPeriodItem(
            "bp-" + ym,
            ym.atDay(1).toString(),
            ym.atEndOfMonth().toString(),
            total.setScale(4, RoundingMode.HALF_UP),
            "open");

    return new BillingDtos.BillingPeriodsResponse(tenant.getId(), List.of(current));
  }

  /**
   * Path tenant must exist and match {@code X-Tenant-Id}. Mismatch / missing header → 404 (no
   * existence leak across tenants).
   */
  private Tenant requireAccessibleTenant(String pathTenantId) {
    String headerTenant = TenantContext.getTenantId();
    if (headerTenant == null || headerTenant.isBlank()) {
      throw new TenantContextRequiredException();
    }
    if (!headerTenant.equals(pathTenantId)) {
      throw new TenantNotFoundException(pathTenantId);
    }
    return tenantRepository
        .findById(pathTenantId)
        .orElseThrow(() -> new TenantNotFoundException(pathTenantId));
  }

  static Instant utcMonthStart(Instant now) {
    return LocalDate.ofInstant(now, ZoneOffset.UTC)
        .withDayOfMonth(1)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant();
  }

  static Instant utcNextMonthStart(Instant now) {
    return LocalDate.ofInstant(now, ZoneOffset.UTC)
        .withDayOfMonth(1)
        .plusMonths(1)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant();
  }
}
