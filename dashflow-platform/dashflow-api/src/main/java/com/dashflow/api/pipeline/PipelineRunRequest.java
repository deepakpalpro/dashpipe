package com.dashflow.api.pipeline;

import com.fasterxml.jackson.databind.JsonNode;

/** Optional body for {@code POST /pipelines/{id}/run}. */
public record PipelineRunRequest(JsonNode payload) {}
