package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record PipelineBundleConnector(
    @JsonProperty("export_key") String exportKey,
    String connectorTypeId,
    String name,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig) {}
