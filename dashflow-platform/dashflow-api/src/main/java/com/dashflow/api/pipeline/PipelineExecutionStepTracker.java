package com.dashflow.api.pipeline;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists per-stage (pipelet) status + timing for pipeline executions.
 *
 * <p>Called by {@link PipelineRunOrchestrator} on run start/cancel and by the runtime workers
 * ({@link StubStageWorker}, {@link com.dashflow.api.k8s.PipeletJobStatusPoller}) as stages run,
 * complete, or fail. Every method is idempotent and never overwrites a terminal stage status.
 */
@Service
public class PipelineExecutionStepTracker {

  private static final Logger log = LoggerFactory.getLogger(PipelineExecutionStepTracker.class);

  private final PipelineExecutionStepRepository stepRepository;

  public PipelineExecutionStepTracker(PipelineExecutionStepRepository stepRepository) {
    this.stepRepository = stepRepository;
  }

  /** Creates one PENDING row per step; stage 1 starts RUNNING with the execution start time. */
  @Transactional
  public void initSteps(PipelineExecution execution, List<PipelineStep> steps) {
    if (steps == null || steps.isEmpty()) {
      return;
    }
    Instant startedAt = execution.getStartedAt() != null ? execution.getStartedAt() : Instant.now();
    List<PipelineExecutionStep> rows = new ArrayList<>(steps.size());
    for (PipelineStep step : steps) {
      PipelineExecutionStep row = new PipelineExecutionStep();
      row.setId(UUID.randomUUID().toString());
      row.setExecutionId(execution.getId());
      row.setPipelineId(execution.getPipelineId());
      row.setTenantId(execution.getTenantId());
      row.setStepOrder(step.getStepOrder());
      row.setPipeletId(step.getPipeletId());
      if (step.getStepOrder() == 1) {
        row.setStatus(ExecutionStatus.RUNNING);
        row.setStartedAt(startedAt);
      } else {
        row.setStatus(ExecutionStatus.PENDING);
      }
      rows.add(row);
    }
    stepRepository.saveAll(rows);
  }

  /** Marks a stage RUNNING (sets started_at once). No-op if the stage is already terminal. */
  @Transactional
  public void markRunning(String executionId, int stepOrder, Instant when) {
    stepRepository
        .findByExecutionIdAndStepOrder(executionId, stepOrder)
        .ifPresent(
            step -> {
              if (isTerminal(step.getStatus()) || step.getStatus() == ExecutionStatus.RUNNING) {
                return;
              }
              step.setStatus(ExecutionStatus.RUNNING);
              if (step.getStartedAt() == null) {
                step.setStartedAt(when != null ? when : Instant.now());
              }
              stepRepository.save(step);
            });
  }

  /** Marks a stage COMPLETED with record counts and timing. No-op if already completed. */
  @Transactional
  public void markCompleted(
      String executionId,
      int stepOrder,
      long recordsIn,
      long recordsOut,
      Instant startedAt,
      Instant completedAt) {
    stepRepository
        .findByExecutionIdAndStepOrder(executionId, stepOrder)
        .ifPresent(
            step -> {
              if (step.getStatus() == ExecutionStatus.COMPLETED) {
                return;
              }
              Instant end = completedAt != null ? completedAt : Instant.now();
              step.setStatus(ExecutionStatus.COMPLETED);
              if (step.getStartedAt() == null) {
                step.setStartedAt(startedAt != null ? startedAt : end);
              }
              step.setCompletedAt(end);
              step.setRecordsIn(recordsIn);
              step.setRecordsOut(recordsOut);
              stepRepository.save(step);
            });
  }

  /** Marks a stage FAILED with an error summary. No-op if already terminal. */
  @Transactional
  public void markFailed(String executionId, int stepOrder, String summary, Instant completedAt) {
    stepRepository
        .findByExecutionIdAndStepOrder(executionId, stepOrder)
        .ifPresent(
            step -> {
              if (isTerminal(step.getStatus())) {
                return;
              }
              step.setStatus(ExecutionStatus.FAILED);
              step.setCompletedAt(completedAt != null ? completedAt : Instant.now());
              step.setErrorSummary(toErrorSummaryJson(summary));
              stepRepository.save(step);
            });
  }

  /** Cancels any stages that have not reached a terminal status (used when an execution stops). */
  @Transactional
  public void cancelRemaining(String executionId, Instant when) {
    Instant at = when != null ? when : Instant.now();
    List<PipelineExecutionStep> steps = stepRepository.findByExecutionIdOrdered(executionId);
    for (PipelineExecutionStep step : steps) {
      if (isTerminal(step.getStatus())) {
        continue;
      }
      step.setStatus(ExecutionStatus.CANCELLED);
      if (step.getStartedAt() != null && step.getCompletedAt() == null) {
        step.setCompletedAt(at);
      }
      stepRepository.save(step);
    }
    if (!steps.isEmpty()) {
      log.debug("Cancelled remaining stages for execution {}", executionId);
    }
  }

  private static boolean isTerminal(ExecutionStatus status) {
    return status == ExecutionStatus.COMPLETED
        || status == ExecutionStatus.FAILED
        || status == ExecutionStatus.CANCELLED;
  }

  private static String toErrorSummaryJson(String summary) {
    String message = summary == null || summary.isBlank() ? "failed" : summary;
    String escaped =
        message
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    return "{\"message\":\"" + escaped + "\"}";
  }
}
