package com.dashflow.api.service;

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
@Table(name = "services")
@TenantOwned
// FilterDef lives on TenantNote (single global definition); only @Filter here.
@Filter(name = TenantFilters.NAME, condition = "tenant_id = :" + TenantFilters.PARAM_TENANT_ID)
public class TenantServiceConfig {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Column(name = "tenant_id", length = 36, nullable = false)
  private String tenantId;

  @Column(name = "service_type_id", length = 36, nullable = false)
  private String serviceTypeId;

  @Column(nullable = false, length = 64)
  private String vendor;

  @Column(nullable = false)
  private String name;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "tenant_config", nullable = false, columnDefinition = "json")
  private String tenantConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "deployment_config", columnDefinition = "json")
  private String deploymentConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "execution_config", columnDefinition = "json")
  private String executionConfig;

  @Column(name = "inherits_default", nullable = false)
  private boolean inheritsDefault = true;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ServiceInstanceStatus status = ServiceInstanceStatus.active;

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

  public String getServiceTypeId() {
    return serviceTypeId;
  }

  public void setServiceTypeId(String serviceTypeId) {
    this.serviceTypeId = serviceTypeId;
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTenantConfig() {
    return tenantConfig;
  }

  public void setTenantConfig(String tenantConfig) {
    this.tenantConfig = tenantConfig;
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

  public boolean isInheritsDefault() {
    return inheritsDefault;
  }

  public void setInheritsDefault(boolean inheritsDefault) {
    this.inheritsDefault = inheritsDefault;
  }

  public ServiceInstanceStatus getStatus() {
    return status;
  }

  public void setStatus(ServiceInstanceStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
