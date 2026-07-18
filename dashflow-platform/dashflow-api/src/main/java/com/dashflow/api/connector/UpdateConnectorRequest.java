package com.dashflow.api.connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateConnectorRequest(
    @NotBlank @Size(max = 255) String name,
    JsonNode config,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig,
    ConnectorInstanceStatus status) {}
