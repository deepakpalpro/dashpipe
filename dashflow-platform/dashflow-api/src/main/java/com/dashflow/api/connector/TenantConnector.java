package com.dashflow.api.connector;

import com.dashflow.api.tenant.TenantFilters;
import com.dashflow.api.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "connectors")
@TenantOwned
@Filter(name = TenantFilters.NAME, condition = "tenant_id = :" + TenantFilters.PARAM_TENANT_ID)
public class TenantConnector {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Column(name = "tenant_id", length = 36, nullable = false)
  private String tenantId;

  @Column(name = "connector_type_id", length = 36, nullable = false)
  private String connectorTypeId;

  @Column(nullable = false)
  private String name;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "json")
  private String config;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "deployment_config", columnDefinition = "json")
  private String deploymentConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "execution_config", columnDefinition = "json")
  private String executionConfig;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConnectorInstanceStatus status = ConnectorInstanceStatus.active;

  @Column(name = "last_tested_at")
  private Instant lastTestedAt;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

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

  public String getConnectorTypeId() {
    return connectorTypeId;
  }

  public void setConnectorTypeId(String connectorTypeId) {
    this.connectorTypeId = connectorTypeId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
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

  public ConnectorInstanceStatus getStatus() {
    return status;
  }

  public void setStatus(ConnectorInstanceStatus status) {
    this.status = status;
  }

  public Instant getLastTestedAt() {
    return lastTestedAt;
  }

  public void setLastTestedAt(Instant lastTestedAt) {
    this.lastTestedAt = lastTestedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
