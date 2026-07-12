package com.dashflow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

/** Portable pipeline bundle (format_version 1). */
public record PipelineBundle(
    @JsonProperty("format_version") String formatVersion,
    @JsonProperty("exported_at") Instant exportedAt,
    PipelineBundlePipeline pipeline,
    List<PipelineBundleStep> steps,
    List<PipelineBundleConnector> connectors,
    List<PipelineBundleServiceEntry> services) {

  public static final String FORMAT_VERSION = "1";
}
