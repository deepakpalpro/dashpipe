package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record PipelineStepResponse(
    String id,
    @JsonProperty("pipelet_id") String pipeletId,
    @JsonProperty("step_order") int stepOrder,
    /** Legacy alias of execution_config for older clients. */
    JsonNode config,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig,
    @JsonProperty("connector_ids") List<String> connectorIds,
    @JsonProperty("service_ids") List<String> serviceIds,
    @JsonProperty("input_queue") String inputQueue,
    @JsonProperty("output_queue") String outputQueue,
    @JsonProperty("resource_limits") JsonNode resourceLimits) {}
