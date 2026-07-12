package com.dashflow.api.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantServiceConfigRepository extends JpaRepository<TenantServiceConfig, String> {

  @Query("select s from TenantServiceConfig s where s.id = :id")
  Optional<TenantServiceConfig> findFilteredById(@Param("id") String id);

  @Query("select s from TenantServiceConfig s order by s.createdAt desc")
  List<TenantServiceConfig> findAllFiltered();

  boolean existsByTenantIdAndName(String tenantId, String name);

  /** Lookup without relying on Hibernate tenant filter (Job env resolution / pollers). */
  @Query("select s from TenantServiceConfig s where s.id = :id and s.tenantId = :tenantId")
  Optional<TenantServiceConfig> findByIdAndTenantId(
      @Param("id") String id, @Param("tenantId") String tenantId);

  /** Public ingress / resolver lookup (no Hibernate tenant filter / no X-Tenant-Id). */
  @Query(
      """
      select s from TenantServiceConfig s
      where s.tenantId = :tenantId
        and s.serviceTypeId = :serviceTypeId
        and s.status = :status
      order by s.createdAt asc
      """)
  List<TenantServiceConfig> findByTenantIdAndServiceTypeIdAndStatus(
      @Param("tenantId") String tenantId,
      @Param("serviceTypeId") String serviceTypeId,
      @Param("status") ServiceInstanceStatus status);
}
