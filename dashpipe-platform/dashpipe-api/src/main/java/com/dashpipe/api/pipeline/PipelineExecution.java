package com.dashpipe.api.pipeline;

import com.dashpipe.api.tenant.TenantFilters;
import com.dashpipe.api.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pipeline_executions")
@TenantOwned
@Filter(name = TenantFilters.NAME, condition = "tenant_id = :" + TenantFilters.PARAM_TENANT_ID)
public class PipelineExecution {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Column(name = "pipeline_id", length = 36, nullable = false)
  private String pipelineId;

  @Column(name = "tenant_id", length = 36, nullable = false)
  private String tenantId;

  @Column(name = "pipeline_version", nullable = false)
  private int pipelineVersion;

  @Convert(converter = ExecutionStatusConverter.class)
  @Column(
      nullable = false,
      columnDefinition = "enum('pending','running','completed','failed','cancelled')")
  private ExecutionStatus status = ExecutionStatus.PENDING;

  @Convert(converter = ExecutionTriggerConverter.class)
  @Column(
      name = "trigger_type",
      nullable = false,
      columnDefinition = "enum('manual','schedule','api')")
  private ExecutionTrigger trigger = ExecutionTrigger.MANUAL;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "records_in", nullable = false)
  private long recordsIn;

  @Column(name = "records_out", nullable = false)
  private long recordsOut;

  @Column(name = "completeness_pct", precision = 5, scale = 2)
  private BigDecimal completenessPct;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "error_summary", columnDefinition = "json")
  private String errorSummary;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public int getPipelineVersion() {
    return pipelineVersion;
  }

  public void setPipelineVersion(int pipelineVersion) {
    this.pipelineVersion = pipelineVersion;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public ExecutionTrigger getTrigger() {
    return trigger;
  }

  public void setTrigger(ExecutionTrigger trigger) {
    this.trigger = trigger;
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

  public long getRecordsIn() {
    return recordsIn;
  }

  public void setRecordsIn(long recordsIn) {
    this.recordsIn = recordsIn;
  }

  public long getRecordsOut() {
    return recordsOut;
  }

  public void setRecordsOut(long recordsOut) {
    this.recordsOut = recordsOut;
  }

  public BigDecimal getCompletenessPct() {
    return completenessPct;
  }

  public void setCompletenessPct(BigDecimal completenessPct) {
    this.completenessPct = completenessPct;
  }

  public String getErrorSummary() {
    return errorSummary;
  }

  public void setErrorSummary(String errorSummary) {
    this.errorSummary = errorSummary;
  }
}
