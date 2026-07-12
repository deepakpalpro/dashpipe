package com.dashflow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record PipelineBundlePipeline(
    String name,
    String description,
    PipelineVisibility visibility,
    @JsonProperty("execution_mode") PipelineExecutionMode executionMode,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig) {}
