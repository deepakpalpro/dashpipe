package com.dashflow.api.pipeline;

import com.dashflow.api.k8s.PipeletJobClient;
import com.dashflow.api.k8s.PipeletJobRequest;
import com.dashflow.api.k8s.PipeletJobRequestFactory;
import com.dashflow.api.messaging.PipelineTopology;
import com.dashflow.api.messaging.PipelineTopologyService;
import com.dashflow.api.messaging.QueueNaming;
import com.dashflow.api.messaging.RabbitMessagingConfig;
import com.dashflow.api.observability.CompletenessCalculator;
import com.dashflow.api.observability.CompletenessMetricsPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Starts async pipeline runs: persist execution, declare topology, publish stage-1 work.
 *
 * <p>Stage advancement is handled by {@link StubStageWorker} when {@code
 * pipeline.orchestration.stub-stage-worker=true} (default). When the stub is disabled (K8s profile),
 * {@link com.dashflow.api.k8s.PipeletJobStatusPoller} advances stages on Job success and
 * marks executions failed on Job backoff.
 */
@Service
public class PipelineRunOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(PipelineRunOrchestrator.class);

  private final PipelineExecutionRepository executionRepository;
  private final PipelineTopologyService topologyService;
  private final RabbitTemplate rabbitTemplate;
  private final PipeletJobClient pipeletJobClient;
  private final CompletenessMetricsPublisher completenessMetricsPublisher;
  private final PipelineOrchestrationProperties orchestrationProperties;
  private final PipeletAmqpUrlFactory amqpUrlFactory;
  private final PipeletJobRequestFactory jobRequestFactory;
  private final PipelineExecutionStepTracker stepTracker;

  public PipelineRunOrchestrator(
      PipelineExecutionRepository executionRepository,
      PipelineTopologyService topologyService,
      RabbitTemplate rabbitTemplate,
      PipeletJobClient pipeletJobClient,
      CompletenessMetricsPublisher completenessMetricsPublisher,
      PipelineOrchestrationProperties orchestrationProperties,
      PipeletAmqpUrlFactory amqpUrlFactory,
      PipeletJobRequestFactory jobRequestFactory,
      PipelineExecutionStepTracker stepTracker) {
    this.executionRepository = executionRepository;
    this.topologyService = topologyService;
    this.rabbitTemplate = rabbitTemplate;
    this.pipeletJobClient = pipeletJobClient;
    this.completenessMetricsPublisher = completenessMetricsPublisher;
    this.orchestrationProperties = orchestrationProperties;
    this.amqpUrlFactory = amqpUrlFactory;
    this.jobRequestFactory = jobRequestFactory;
    this.stepTracker = stepTracker;
  }

  @Transactional
  public PipelineExecution start(
      Pipeline pipeline, List<PipelineStep> steps, ExecutionTrigger trigger) {
    return start(pipeline, steps, trigger, null);
  }

  @Transactional
  public PipelineExecution start(
      Pipeline pipeline,
      List<PipelineStep> steps,
      ExecutionTrigger trigger,
      JsonNode triggerPayload) {
    if (steps == null || steps.isEmpty()) {
      throw new PipelineValidationException("Pipeline has no steps configured");
    }
    if (pipeline.getStatus() == PipelineStatus.ARCHIVED) {
      throw new PipelineValidationException("Cannot run an archived pipeline");
    }
    if (pipeline.getStatus() != PipelineStatus.ACTIVE) {
      throw new PipelineValidationException("Activate the pipeline before running (status=active)");
    }

    String ioMode = PipelineIoMode.fromExecutionConfigJson(pipeline.getExecutionConfig());
    String amqpUrl = amqpUrlFactory.resolve();

    PipelineExecution execution = new PipelineExecution();
    execution.setId(UUID.randomUUID().toString());
    execution.setPipelineId(pipeline.getId());
    execution.setTenantId(pipeline.getTenantId());
    execution.setPipelineVersion(pipeline.getVersion());
    execution.setStatus(ExecutionStatus.PENDING);
    execution.setTrigger(trigger == null ? ExecutionTrigger.MANUAL : trigger);
    execution.setStartedAt(Instant.now());
    execution.setRecordsIn(0);
    execution.setRecordsOut(0);
    PipelineExecution saved = executionRepository.save(execution);
    stepTracker.initSteps(saved, steps);

    int stageCount = steps.size();
    PipelineTopology topology =
        topologyService.declare(pipeline.getTenantId(), pipeline.getId(), stageCount);
    // Prior runs (and unused source kickoffs) leave messages that the next Job would consume first.
    topologyService.purgeStageQueues(topology);

    PipelineStep first = steps.get(0);
    PipeletJobRequest jobRequest =
        jobRequestFactory.build(pipeline, first, saved.getId(), stageCount, ioMode, amqpUrl);
    String payloadJson = serializeTriggerPayload(triggerPayload);
    if (payloadJson != null) {
      jobRequest = jobRequest.withTriggerPayload(payloadJson);
    }
    pipeletJobClient.create(jobRequest);

    StageMessage message =
        new StageMessage(
            saved.getId(),
            pipeline.getId(),
            pipeline.getTenantId(),
            first.getPipeletId(),
            1,
            stageCount,
            "run-" + saved.getId(),
            ioMode,
            amqpUrl);
    // Queue-mode sources use SOURCE_TRIGGER=once and do not consume stage-1 kickoff. Publishing a
    // StageMessage with payload "run-…" onto stage queues confuses CSV parsers if it leaks or is
    // left over — only notify the stub worker path (if enabled).
    if (orchestrationProperties.isStubStageWorker()) {
      rabbitTemplate.convertAndSend(topology.exchange(), QueueNaming.stageRoutingKey(1), message);
      rabbitTemplate.convertAndSend(RabbitMessagingConfig.STUB_STAGE_WORKER_QUEUE, message);
    } else if (!PipelineIoMode.QUEUE.equals(PipelineIoMode.normalize(ioMode))) {
      rabbitTemplate.convertAndSend(topology.exchange(), QueueNaming.stageRoutingKey(1), message);
    }

    saved.setStatus(ExecutionStatus.RUNNING);
    saved = executionRepository.save(saved);

    log.info(
        "Started execution {} for pipeline {} ({} stages, ioMode={})",
        saved.getId(),
        pipeline.getId(),
        stageCount,
        ioMode);
    return saved;
  }

  private static String serializeTriggerPayload(JsonNode triggerPayload) {
    if (triggerPayload == null || triggerPayload.isNull() || triggerPayload.isMissingNode()) {
      return null;
    }
    return triggerPayload.toString();
  }

  @Transactional
  public void markCompleted(String executionId, long recordsIn, long recordsOut) {
    executionRepository
        .findById(executionId)
        .ifPresent(
            execution -> {
              if (isTerminal(execution.getStatus())) {
                return;
              }
              CompletenessCalculator.Result completeness =
                  CompletenessCalculator.calculate(recordsIn, recordsOut);
              execution.setStatus(ExecutionStatus.COMPLETED);
              execution.setCompletedAt(Instant.now());
              execution.setRecordsIn(completeness.recordsIn());
              execution.setRecordsOut(completeness.recordsOut());
              execution.setCompletenessPct(completeness.percent());
              executionRepository.save(execution);
              completenessMetricsPublisher.publish(
                  execution.getTenantId(), execution.getPipelineId(), completeness.ratio());
            });
  }

  @Transactional
  public void markFailed(String executionId, String summary) {
    executionRepository
        .findById(executionId)
        .ifPresent(
            execution -> {
              if (isTerminal(execution.getStatus())) {
                return;
              }
              execution.setStatus(ExecutionStatus.FAILED);
              execution.setCompletedAt(Instant.now());
              execution.setErrorSummary(toErrorSummaryJson(summary));
              executionRepository.save(execution);
              stepTracker.cancelRemaining(executionId, Instant.now());
              log.warn("Marked execution {} failed: {}", executionId, summary);
            });
  }

  /**
   * Force-stops a non-terminal execution: marks {@code CANCELLED}, deletes Jobs, purges stage
   * queues so the next Run starts clean.
   */
  @Transactional
  public PipelineExecution cancel(PipelineExecution execution, int stageCount) {
    if (execution == null) {
      throw new IllegalArgumentException("execution is required");
    }
    if (isTerminal(execution.getStatus())) {
      return execution;
    }

    execution.setStatus(ExecutionStatus.CANCELLED);
    execution.setCompletedAt(Instant.now());
    execution.setErrorSummary(toErrorSummaryJson("Force stopped by user"));
    PipelineExecution saved = executionRepository.save(execution);
    stepTracker.cancelRemaining(saved.getId(), Instant.now());

    try {
      pipeletJobClient.deleteByExecution(saved.getTenantId(), saved.getId());
    } catch (Exception ex) {
      log.warn(
          "Job cleanup failed during cancel of {}: {}", saved.getId(), ex.getMessage());
    }

    int stages = Math.max(1, stageCount);
    try {
      PipelineTopology topology =
          topologyService.declare(saved.getTenantId(), saved.getPipelineId(), stages);
      topologyService.purgeStageQueues(topology);
    } catch (Exception ex) {
      log.warn(
          "Queue purge failed during cancel of {}: {}", saved.getId(), ex.getMessage());
    }

    log.info("Cancelled execution {} for pipeline {}", saved.getId(), saved.getPipelineId());
    return saved;
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

  private static boolean isTerminal(ExecutionStatus status) {
    return status == ExecutionStatus.COMPLETED
        || status == ExecutionStatus.FAILED
        || status == ExecutionStatus.CANCELLED;
  }
}
