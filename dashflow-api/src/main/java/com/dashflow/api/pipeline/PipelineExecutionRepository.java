package com.dashflow.api.pipeline;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PipelineExecutionRepository extends JpaRepository<PipelineExecution, String> {

  @Query("select e from PipelineExecution e where e.id = :id")
  Optional<PipelineExecution> findFilteredById(@Param("id") String id);

  @Query(
      "select e from PipelineExecution e where e.pipelineId = :pipelineId order by e.startedAt desc")
  List<PipelineExecution> findFilteredByPipelineId(@Param("pipelineId") String pipelineId);

  /** Background pollers (no tenant filter required — status scoped). */
  @Query("select e from PipelineExecution e where e.status = :status order by e.startedAt asc")
  List<PipelineExecution> findByStatus(@Param("status") ExecutionStatus status);
}
