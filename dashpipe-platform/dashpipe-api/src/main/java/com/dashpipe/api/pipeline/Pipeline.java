package com.dashpipe.api.pipeline;

import com.dashpipe.api.tenant.TenantFilters;
import com.dashpipe.api.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pipelines")
@TenantOwned
@Filter(name = TenantFilters.NAME, condition = "tenant_id = :" + TenantFilters.PARAM_TENANT_ID)
public class Pipeline {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Column(name = "tenant_id", length = 36, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Convert(converter = PipelineVisibilityConverter.class)
  @Column(nullable = false, columnDefinition = "enum('public','private')")
  private PipelineVisibility visibility = PipelineVisibility.PRIVATE;

  @Convert(converter = PipelineExecutionModeConverter.class)
  @Column(name = "execution_mode", nullable = false, columnDefinition = "enum('async','sync')")
  private PipelineExecutionMode executionMode = PipelineExecutionMode.ASYNC;

  @Column(nullable = false)
  private int version = 1;

  @Convert(converter = PipelineStatusConverter.class)
  @Column(nullable = false, columnDefinition = "enum('draft','active','archived')")
  private PipelineStatus status = PipelineStatus.DRAFT;

  @Column(name = "schedule_cron", length = 64)
  private String scheduleCron;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "retry_config", columnDefinition = "json")
  private String retryConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "deployment_config", columnDefinition = "json")
  private String deploymentConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "execution_config", columnDefinition = "json")
  private String executionConfig;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private Instant updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public PipelineVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(PipelineVisibility visibility) {
    this.visibility = visibility;
  }

  public PipelineExecutionMode getExecutionMode() {
    return executionMode;
  }

  public void setExecutionMode(PipelineExecutionMode executionMode) {
    this.executionMode = executionMode;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public PipelineStatus getStatus() {
    return status;
  }

  public void setStatus(PipelineStatus status) {
    this.status = status;
  }

  public String getScheduleCron() {
    return scheduleCron;
  }

  public void setScheduleCron(String scheduleCron) {
    this.scheduleCron = scheduleCron;
  }

  public String getRetryConfig() {
    return retryConfig;
  }

  public void setRetryConfig(String retryConfig) {
    this.retryConfig = retryConfig;
  }

  public String getDeploymentConfig() {
    return deploymentConfig;
  }

  public void setDeploymentConfig(String deploymentConfig) {
    this.deploymentConfig = deploymentConfig;
  }

  public String getExecutionConfig() {
    return executionConfig;
  }

  public void setExecutionConfig(String executionConfig) {
    this.executionConfig = executionConfig;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
