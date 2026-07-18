package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PipelineBundleImportRequest(
    PipelineBundle bundle,
    /** Optional override for the imported pipeline name. */
    String name,
    /**
     * {@code create} (default) always creates connectors/services with unique names; {@code reuse}
     * matches existing by type+name (or type+vendor+name for services) when present.
     */
    @JsonProperty("conflict_strategy") String conflictStrategy) {

  public record Result(
      @JsonProperty("pipeline_id") String pipelineId,
      String name,
      @JsonProperty("created_connectors") List<String> createdConnectors,
      @JsonProperty("reused_connectors") List<String> reusedConnectors,
      @JsonProperty("created_services") List<String> createdServices,
      @JsonProperty("reused_services") List<String> reusedServices,
      List<String> warnings) {}
}
