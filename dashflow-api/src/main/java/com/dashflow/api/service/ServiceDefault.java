package com.dashflow.api.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "service_defaults")
public class ServiceDefault {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_type_id", nullable = false)
  private ServiceType serviceType;

  @Column(nullable = false, length = 64)
  private String vendor;

  @Column(name = "base_service_class", length = 512)
  private String baseServiceClass;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "default_config", nullable = false, columnDefinition = "json")
  private String defaultConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "default_deployment_config", columnDefinition = "json")
  private String defaultDeploymentConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "default_execution_config", columnDefinition = "json")
  private String defaultExecutionConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "config_schema", columnDefinition = "json")
  private String configSchema;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ServiceType getServiceType() {
    return serviceType;
  }

  public void setServiceType(ServiceType serviceType) {
    this.serviceType = serviceType;
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public String getBaseServiceClass() {
    return baseServiceClass;
  }

  public void setBaseServiceClass(String baseServiceClass) {
    this.baseServiceClass = baseServiceClass;
  }

  public String getDefaultConfig() {
    return defaultConfig;
  }

  public void setDefaultConfig(String defaultConfig) {
    this.defaultConfig = defaultConfig;
  }

  public String getDefaultDeploymentConfig() {
    return defaultDeploymentConfig;
  }

  public void setDefaultDeploymentConfig(String defaultDeploymentConfig) {
    this.defaultDeploymentConfig = defaultDeploymentConfig;
  }

  public String getDefaultExecutionConfig() {
    return defaultExecutionConfig;
  }

  public void setDefaultExecutionConfig(String defaultExecutionConfig) {
    this.defaultExecutionConfig = defaultExecutionConfig;
  }

  public String getConfigSchema() {
    return configSchema;
  }

  public void setConfigSchema(String configSchema) {
    this.configSchema = configSchema;
  }
}
