package com.dashflow.api.billing;

import com.dashflow.api.tenant.Tenant;
import com.dashflow.api.tenant.TenantNotFoundException;
import com.dashflow.api.tenant.TenantRepository;
import com.dashflow.api.usage.UsageAggregateEntity;
import com.dashflow.api.usage.UsageAggregateRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads tenant credit + quota_config and period usage, then evaluates. Soft warnings are log-only in
 * Wave 5.
 */
@Service
public class QuotaService {

  private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

  private final TenantRepository tenantRepository;
  private final UsageAggregateRepository aggregateRepository;
  private final QuotaConfigParser configParser;
  private final QuotaEvaluator evaluator;
  private final Clock clock;

  public QuotaService(
      TenantRepository tenantRepository,
      UsageAggregateRepository aggregateRepository,
      QuotaConfigParser configParser,
      QuotaEvaluator evaluator,
      Clock clock) {
    this.tenantRepository = tenantRepository;
    this.aggregateRepository = aggregateRepository;
    this.configParser = configParser;
    this.evaluator = evaluator;
    this.clock = clock;
  }

  /**
   * Evaluate against credit and UTC calendar-month hourly aggregate totals (Wave 5 stub period).
   */
  @Transactional(readOnly = true)
  public QuotaDecision evaluateTenant(String tenantId) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

    Instant now = clock.instant();
    Instant monthStart =
        LocalDate.ofInstant(now, ZoneOffset.UTC)
            .withDayOfMonth(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant();

    Map<String, BigDecimal> usage = sumHourlyUsage(tenantId, monthStart, now);
    QuotaConfig config = configParser.parse(tenant.getQuotaConfig());
    QuotaDecision decision = evaluator.evaluate(tenant.getCreditBalance(), config, usage);

    if (decision.code() == QuotaDecisionCode.SOFT_WARN) {
      log.warn(
          "quota soft warn tenantId={} dimension={} usage={} soft={} hard={}",
          tenantId,
          decision.breachedDimension(),
          decision.currentUsage(),
          decision.softLimit(),
          decision.hardLimit());
    }
    return decision;
  }

  /** Evaluate with explicit inputs (tests / US06 gate with preloaded usage). */
  public QuotaDecision evaluate(
      BigDecimal creditBalance, String quotaConfigJson, Map<String, BigDecimal> usageByDimension) {
    return evaluator.evaluate(
        creditBalance, configParser.parse(quotaConfigJson), usageByDimension);
  }

  private Map<String, BigDecimal> sumHourlyUsage(
      String tenantId, Instant periodStart, Instant periodEnd) {
    List<UsageAggregateEntity> rows =
        aggregateRepository
            .findByTenantIdAndGranularityAndPeriodStartGreaterThanEqualAndPeriodStartLessThan(
                tenantId, UsageAggregateEntity.GRANULARITY_HOURLY, periodStart, periodEnd);
    Map<String, BigDecimal> totals = new HashMap<>();
    for (UsageAggregateEntity row : rows) {
      totals.merge(
          row.getDimension(),
          row.getTotalQuantity() == null ? BigDecimal.ZERO : row.getTotalQuantity(),
          BigDecimal::add);
    }
    return totals;
  }
}
