package com.dashflow.api.pipeline;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PipelineStepRepository extends JpaRepository<PipelineStep, String> {

  @Query("select s from PipelineStep s where s.pipelineId = :pipelineId order by s.stepOrder asc")
  List<PipelineStep> findByPipelineIdOrdered(@Param("pipelineId") String pipelineId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from PipelineStep s where s.pipelineId = :pipelineId")
  void deleteByPipelineId(@Param("pipelineId") String pipelineId);
}
