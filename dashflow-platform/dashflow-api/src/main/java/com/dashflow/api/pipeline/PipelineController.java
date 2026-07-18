package com.dashflow.api.pipeline;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipelines")
public class PipelineController {

  private final PipelineService pipelineService;
  private final PipelineStepsService pipelineStepsService;
  private final PipelineRunService pipelineRunService;
  private final PipelineDryRunService pipelineDryRunService;
  private final PipelineBundleService pipelineBundleService;

  public PipelineController(
      PipelineService pipelineService,
      PipelineStepsService pipelineStepsService,
      PipelineRunService pipelineRunService,
      PipelineDryRunService pipelineDryRunService,
      PipelineBundleService pipelineBundleService) {
    this.pipelineService = pipelineService;
    this.pipelineStepsService = pipelineStepsService;
    this.pipelineRunService = pipelineRunService;
    this.pipelineDryRunService = pipelineDryRunService;
    this.pipelineBundleService = pipelineBundleService;
  }

  @PostMapping
  public ResponseEntity<PipelineResponse> create(@Valid @RequestBody CreatePipelineRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(pipelineService.create(request));
  }

  @PostMapping("/import")
  public ResponseEntity<PipelineBundleImportRequest.Result> importBundle(
      @Valid @RequestBody PipelineBundleImportRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(pipelineBundleService.importBundle(request));
  }

  @GetMapping("/{id}/export")
  public PipelineBundle export(@PathVariable String id) {
    return pipelineBundleService.export(id);
  }

  @GetMapping("/{id}")
  public PipelineResponse get(@PathVariable String id) {
    return pipelineService.get(id);
  }

  @GetMapping
  public List<PipelineResponse> list() {
    return pipelineService.list();
  }

  @PutMapping("/{id}")
  public PipelineResponse update(
      @PathVariable String id, @Valid @RequestBody UpdatePipelineRequest request) {
    return pipelineService.update(id, request);
  }

  @PutMapping("/{id}/steps")
  public PipelineResponse replaceSteps(
      @PathVariable String id, @Valid @RequestBody ReplacePipelineStepsRequest request) {
    return pipelineStepsService.replace(id, request);
  }

  @PostMapping("/{id}/dry-run")
  public PipelineDryRunResponse dryRun(@PathVariable String id) {
    return pipelineDryRunService.dryRun(id);
  }

  @PostMapping("/{id}/run")
  public ResponseEntity<PipelineRunResponse> run(
      @PathVariable String id, @RequestBody(required = false) PipelineRunRequest body) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(pipelineRunService.run(id, body == null ? null : body.payload()));
  }

  @GetMapping("/{id}/executions")
  public List<PipelineExecutionResponse> listExecutions(@PathVariable String id) {
    return pipelineRunService.listExecutions(id);
  }

  @GetMapping("/{id}/executions/{executionId}")
  public PipelineExecutionResponse getExecution(
      @PathVariable String id, @PathVariable String executionId) {
    return pipelineRunService.getExecution(id, executionId);
  }

  @PostMapping("/{id}/executions/{executionId}/cancel")
  public PipelineExecutionResponse cancelExecution(
      @PathVariable String id, @PathVariable String executionId) {
    return pipelineRunService.cancel(id, executionId);
  }

  @DeleteMapping("/{id}")
  public PipelineResponse archive(@PathVariable String id) {
    return pipelineService.archive(id);
  }
}
