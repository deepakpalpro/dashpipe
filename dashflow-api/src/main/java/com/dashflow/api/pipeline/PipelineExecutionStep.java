package com.dashflow.api.pipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Per-stage (pipelet) status + timing for one {@link PipelineExecution}. One row per pipeline step
 * per execution. Created PENDING by {@link PipelineRunOrchestrator} on run start and advanced by
 * {@link StubStageWorker} / {@link com.dashflow.api.k8s.PipeletJobStatusPoller} as stages run,
 * complete, or fail.
 */
@Entity
@Table(name = "pipeline_execution_steps")
public class PipelineExecutionStep {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Column(name = "execution_id", length = 36, nullable = false)
  private String executionId;

  @Column(name = "pipeline_id", length = 36, nullable = false)
  private String pipelineId;

  @Column(name = "tenant_id", length = 36, nullable = false)
  private String tenantId;

  @Column(name = "step_order", nullable = false)
  private int stepOrder;

  @Column(name = "pipelet_id", length = 64, nullable = false)
  private String pipeletId;

  @Convert(converter = ExecutionStatusConverter.class)
  @Column(
      nullable = false,
      columnDefinition = "enum('pending','running','completed','failed','cancelled')")
  private ExecutionStatus status = ExecutionStatus.PENDING;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "records_in")
  private Long recordsIn;

  @Column(name = "records_out")
  private Long recordsOut;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "error_summary", columnDefinition = "json")
  private String errorSummary;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public String getPipelineId() {
    return pipelineId;
  }

  public void setPipelineId(String pipelineId) {
    this.pipelineId = pipelineId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public int getStepOrder() {
    return stepOrder;
  }

  public void setStepOrder(int stepOrder) {
    this.stepOrder = stepOrder;
  }

  public String getPipeletId() {
    return pipeletId;
  }

  public void setPipeletId(String pipeletId) {
    this.pipeletId = pipeletId;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public Long getRecordsIn() {
    return recordsIn;
  }

  public void setRecordsIn(Long recordsIn) {
    this.recordsIn = recordsIn;
  }

  public Long getRecordsOut() {
    return recordsOut;
  }

  public void setRecordsOut(Long recordsOut) {
    this.recordsOut = recordsOut;
  }

  public String getErrorSummary() {
    return errorSummary;
  }

  public void setErrorSummary(String errorSummary) {
    this.errorSummary = errorSummary;
  }
}
