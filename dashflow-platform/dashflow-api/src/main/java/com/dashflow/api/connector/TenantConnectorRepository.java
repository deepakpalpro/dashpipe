package com.dashflow.api.connector;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantConnectorRepository extends JpaRepository<TenantConnector, String> {

  @Query("select c from TenantConnector c where c.id = :id")
  Optional<TenantConnector> findFilteredById(@Param("id") String id);

  @Query("select c from TenantConnector c order by c.createdAt desc")
  List<TenantConnector> findAllFiltered();

  boolean existsByTenantIdAndName(String tenantId, String name);

  /** Public webhook ingress lookup (no Hibernate tenant filter / no X-Tenant-Id). */
  @Query("select c from TenantConnector c where c.id = :id and c.tenantId = :tenantId")
  Optional<TenantConnector> findByIdAndTenantId(
      @Param("id") String id, @Param("tenantId") String tenantId);
}
