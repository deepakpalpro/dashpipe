package com.dashflow.api.connector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "connector_types")
public class ConnectorType {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConnectorKind type;

  @Column(name = "display_name", nullable = false, length = 128)
  private String displayName;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "config_schema", columnDefinition = "json")
  private String configSchema;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "default_deployment_config", columnDefinition = "json")
  private String defaultDeploymentConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "default_execution_config", columnDefinition = "json")
  private String defaultExecutionConfig;

  @Column(name = "spi_class", nullable = false, length = 512)
  private String spiClass;

  @Column(name = "spi_version", nullable = false, length = 16)
  private String spiVersion;

  public String getId() {
    return id;
  }

  public ConnectorKind getType() {
    return type;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getConfigSchema() {
    return configSchema;
  }

  public String getDefaultDeploymentConfig() {
    return defaultDeploymentConfig;
  }

  public String getDefaultExecutionConfig() {
    return defaultExecutionConfig;
  }

  public String getSpiClass() {
    return spiClass;
  }

  public String getSpiVersion() {
    return spiVersion;
  }
}
