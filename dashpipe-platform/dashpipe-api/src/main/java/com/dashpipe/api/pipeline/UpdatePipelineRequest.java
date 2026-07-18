package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePipelineRequest(
    @NotBlank @Size(max = 255) String name,
    String description,
    PipelineVisibility visibility,
    PipelineExecutionMode executionMode,
    PipelineStatus status,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig) {}
