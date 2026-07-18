package com.dashpipe.api.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTenantServiceRequest(
    @NotBlank String serviceTypeId,
    @NotBlank @Size(max = 64) String vendor,
    @NotBlank @Size(max = 255) String name,
    @NotNull JsonNode tenantConfig,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig,
    Boolean inheritsDefault) {}
