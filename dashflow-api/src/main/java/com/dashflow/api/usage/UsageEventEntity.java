package com.dashflow.api.usage;

import com.dashflow.api.tenant.TenantFilters;
import com.dashflow.api.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usage_events")
@TenantOwned
@Filter(name = TenantFilters.NAME, condition = "tenant_id = :" + TenantFilters.PARAM_TENANT_ID)
public class UsageEventEntity {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Column(name = "tenant_id", length = 36, nullable = false)
  private String tenantId;

  @Column(name = "execution_id", length = 36)
  private String executionId;

  @Column(name = "pipeline_id", length = 36)
  private String pipelineId;

  @Column(name = "pipelet_id", length = 36)
  private String pipeletId;

  @Column(name = "connector_id", length = 36)
  private String connectorId;

  @Column(nullable = false, length = 64)
  private String dimension;

  @Column(nullable = false, precision = 18, scale = 6)
  private BigDecimal quantity;

  @Column(length = 32)
  private String unit;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private String metadata;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  @Column(name = "idempotency_key", length = 191, unique = true)
  private String idempotencyKey;

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

  public String getPipeletId() {
    return pipeletId;
  }

  public void setPipeletId(String pipeletId) {
    this.pipeletId = pipeletId;
  }

  public String getConnectorId() {
    return connectorId;
  }

  public void setConnectorId(String connectorId) {
    this.connectorId = connectorId;
  }

  public String getDimension() {
    return dimension;
  }

  public void setDimension(String dimension) {
    this.dimension = dimension;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  public void setRecordedAt(Instant recordedAt) {
    this.recordedAt = recordedAt;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }
}
