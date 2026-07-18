package com.dashpipe.api.pipeline;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PipelineExecutionStepRepository
    extends JpaRepository<PipelineExecutionStep, String> {

  @Query(
      "select s from PipelineExecutionStep s where s.executionId = :executionId"
          + " order by s.stepOrder asc")
  List<PipelineExecutionStep> findByExecutionIdOrdered(@Param("executionId") String executionId);

  @Query(
      "select s from PipelineExecutionStep s where s.executionId = :executionId"
          + " and s.stepOrder = :stepOrder")
  Optional<PipelineExecutionStep> findByExecutionIdAndStepOrder(
      @Param("executionId") String executionId, @Param("stepOrder") int stepOrder);
}
