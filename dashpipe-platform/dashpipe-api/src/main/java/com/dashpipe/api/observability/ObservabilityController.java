package com.dashpipe.api.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/observability")
public class ObservabilityController {

  private final ObservabilityService observabilityService;

  public ObservabilityController(ObservabilityService observabilityService) {
    this.observabilityService = observabilityService;
  }

  @GetMapping("/pipelines/{id}/completeness")
  public ObservabilityDtos.CompletenessResponse completeness(@PathVariable("id") String pipelineId) {
    return observabilityService.completeness(pipelineId);
  }

  @GetMapping("/pipelines/{id}/latency")
  public ObservabilityDtos.LatencyResponse latency(@PathVariable("id") String pipelineId) {
    return observabilityService.latency(pipelineId);
  }

  @GetMapping("/pipelines/{id}/heartbeat")
  public ObservabilityDtos.HeartbeatResponse heartbeat(@PathVariable("id") String pipelineId) {
    return observabilityService.heartbeat(pipelineId);
  }

  @GetMapping("/pipelines/{id}/errors")
  public ObservabilityDtos.ErrorSummaryResponse errors(@PathVariable("id") String pipelineId) {
    return observabilityService.errors(pipelineId);
  }

  @GetMapping("/executions/{execId}/logs")
  public ObservabilityDtos.ExecutionLogsResponse logs(@PathVariable("execId") String execId) {
    return observabilityService.logs(execId);
  }
}
