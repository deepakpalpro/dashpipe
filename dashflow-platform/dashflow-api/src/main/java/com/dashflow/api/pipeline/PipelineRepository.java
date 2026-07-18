package com.dashflow.api.pipeline;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PipelineRepository extends JpaRepository<Pipeline, String> {

  @Query("select p from Pipeline p where p.id = :id")
  Optional<Pipeline> findFilteredById(@Param("id") String id);

  @Query("select p from Pipeline p order by p.createdAt desc")
  List<Pipeline> findAllFiltered();

  boolean existsByTenantIdAndName(String tenantId, String name);

  boolean existsByTenantIdAndNameAndIdNot(String tenantId, String name, String id);

  @Query("select p from Pipeline p where p.status = :status order by p.updatedAt asc")
  List<Pipeline> findByStatus(@Param("status") PipelineStatus status);
}
