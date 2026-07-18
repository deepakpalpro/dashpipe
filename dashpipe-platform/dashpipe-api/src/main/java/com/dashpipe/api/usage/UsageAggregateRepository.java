package com.dashpipe.api.usage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageAggregateRepository extends JpaRepository<UsageAggregateEntity, String> {

  Optional<UsageAggregateEntity> findByTenantIdAndDimensionAndGranularityAndPeriodStart(
      String tenantId, String dimension, String granularity, Instant periodStart);

  List<UsageAggregateEntity> findByTenantIdAndGranularityAndPeriodStart(
      String tenantId, String granularity, Instant periodStart);

  List<UsageAggregateEntity>
      findByTenantIdAndGranularityAndPeriodStartGreaterThanEqualAndPeriodStartLessThan(
          String tenantId, String granularity, Instant periodStartInclusive, Instant periodEndExclusive);
}
