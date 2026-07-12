package com.dashflow.api.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record TenantServiceResponse(
    String id,
    String tenantId,
    String serviceTypeId,
    String vendor,
    String name,
    JsonNode config,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig,
    boolean inheritsDefault,
    ServiceInstanceStatus status,
    Instant createdAt) {}
