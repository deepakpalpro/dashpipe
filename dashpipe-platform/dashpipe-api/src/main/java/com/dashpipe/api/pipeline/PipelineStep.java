package com.dashpipe.api.pipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pipeline_steps")
public class PipelineStep {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Column(name = "pipeline_id", length = 36, nullable = false)
  private String pipelineId;

  @Column(name = "pipelet_id", length = 36, nullable = false)
  private String pipeletId;

  @Column(name = "step_order", nullable = false)
  private int stepOrder;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private String config;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "deployment_config", columnDefinition = "json")
  private String deploymentConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "execution_config", columnDefinition = "json")
  private String executionConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "connector_ids", columnDefinition = "json")
  private String connectorIds;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "service_ids", columnDefinition = "json")
  private String serviceIds;

  @Column(name = "input_queue")
  private String inputQueue;

  @Column(name = "output_queue")
  private String outputQueue;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "resource_limits", columnDefinition = "json")
  private String resourceLimits;

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

  public String getPipeletId() {
    return pipeletId;
  }

  public void setPipeletId(String pipeletId) {
    this.pipeletId = pipeletId;
  }

  public int getStepOrder() {
    return stepOrder;
  }

  public void setStepOrder(int stepOrder) {
    this.stepOrder = stepOrder;
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

  public String getConnectorIds() {
    return connectorIds;
  }

  public void setConnectorIds(String connectorIds) {
    this.connectorIds = connectorIds;
  }

  public String getServiceIds() {
    return serviceIds;
  }

  public void setServiceIds(String serviceIds) {
    this.serviceIds = serviceIds;
  }

  public String getInputQueue() {
    return inputQueue;
  }

  public void setInputQueue(String inputQueue) {
    this.inputQueue = inputQueue;
  }

  public String getOutputQueue() {
    return outputQueue;
  }

  public void setOutputQueue(String outputQueue) {
    this.outputQueue = outputQueue;
  }

  public String getResourceLimits() {
    return resourceLimits;
  }

  public void setResourceLimits(String resourceLimits) {
    this.resourceLimits = resourceLimits;
  }
}
