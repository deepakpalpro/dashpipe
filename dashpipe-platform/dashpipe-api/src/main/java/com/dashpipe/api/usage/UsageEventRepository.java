package com.dashpipe.api.usage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageEventRepository extends JpaRepository<UsageEventEntity, String> {

  Optional<UsageEventEntity> findByIdempotencyKey(String idempotencyKey);

  @Query(
      "select e from UsageEventEntity e where e.tenantId = :tenantId order by e.recordedAt desc")
  List<UsageEventEntity> findByTenantIdOrderByRecordedAtDesc(@Param("tenantId") String tenantId);

  Page<UsageEventEntity> findByTenantIdOrderByRecordedAtDesc(String tenantId, Pageable pageable);

  long countByTenantId(String tenantId);

  @Query(
      """
      select e.tenantId as tenantId, e.dimension as dimension, sum(e.quantity) as totalQuantity
      from UsageEventEntity e
      where e.recordedAt >= :periodStart and e.recordedAt < :periodEnd
      group by e.tenantId, e.dimension
      """)
  List<UsageDimensionSum> sumByTenantAndDimensionForPeriod(
      @Param("periodStart") Instant periodStart, @Param("periodEnd") Instant periodEnd);
}
