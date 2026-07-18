package com.dashflow.api.pipeline;

import java.util.List;

/** Response for {@code POST /api/v1/pipelines/{id}/dry-run}. */
public record PipelineDryRunResponse(boolean valid, List<String> messages) {

  static PipelineDryRunResponse ok(List<String> messages) {
    return new PipelineDryRunResponse(true, List.copyOf(messages));
  }

  static PipelineDryRunResponse invalid(List<String> messages) {
    return new PipelineDryRunResponse(false, List.copyOf(messages));
  }
}
