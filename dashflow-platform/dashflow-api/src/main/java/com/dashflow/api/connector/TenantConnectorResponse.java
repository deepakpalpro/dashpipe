package com.dashflow.api.connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record TenantConnectorResponse(
    String id,
    String tenantId,
    String connectorTypeId,
    String name,
    JsonNode config,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig,
    ConnectorInstanceStatus status,
    Instant lastTestedAt,
    Instant createdAt) {}
