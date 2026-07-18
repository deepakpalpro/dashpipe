package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/** Tenant service config entry inside a portable pipeline bundle. */
public record PipelineBundleServiceEntry(
    @JsonProperty("export_key") String exportKey,
    String serviceTypeId,
    String vendor,
    String name,
    Boolean inheritsDefault,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig) {}
