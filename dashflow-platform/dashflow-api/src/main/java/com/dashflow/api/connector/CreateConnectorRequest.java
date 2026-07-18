package com.dashflow.api.connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateConnectorRequest(
    @NotBlank String connectorTypeId,
    @NotBlank @Size(max = 255) String name,
    @NotNull JsonNode config,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig) {}
