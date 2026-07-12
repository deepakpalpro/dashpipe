package com.dashflow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Step payload. {@code config} is a legacy alias of {@code execution_config}. */
public record PipelineStepRequest(
    @JsonProperty("pipelet_id") @NotBlank @Size(max = 36) String pipeletId,
    @JsonProperty("step_order") @NotNull @Min(1) Integer stepOrder,
    JsonNode config,
    @JsonProperty("deployment_config") JsonNode deploymentConfig,
    @JsonProperty("execution_config") JsonNode executionConfig,
    @JsonProperty("connector_ids") List<String> connectorIds,
    @JsonProperty("service_ids") List<String> serviceIds,
    @JsonProperty("input_queue") String inputQueue,
    @JsonProperty("output_queue") String outputQueue,
    @JsonProperty("resource_limits") JsonNode resourceLimits) {}
