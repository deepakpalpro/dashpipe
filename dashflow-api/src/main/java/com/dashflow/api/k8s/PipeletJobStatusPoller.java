package com.dashflow.api.k8s;

import com.dashflow.api.observability.PipeletErrorType;
import com.dashflow.api.observability.PipeletMetricsEmitter;
import com.dashflow.api.observability.PipelineLogEmitter;
import com.dashflow.api.pipeline.ExecutionStatus;
import com.dashflow.api.pipeline.Pipeline;
import com.dashflow.api.pipeline.PipelineExecution;
import com.dashflow.api.pipeline.PipelineExecutionRepository;
import com.dashflow.api.pipeline.PipelineIoMode;
import com.dashflow.api.pipeline.PipelineOrchestrationProperties;
import com.dashflow.api.pipeline.PipelineRepository;
import com.dashflow.api.pipeline.PipelineRunOrchestrator;
import com.dashflow.api.pipeline.PipelineStep;
import com.dashflow.api.pipeline.PipelineStepRepository;
import com.dashflow.api.pipeline.PipeletAmqpUrlFactory;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * When real K8s Jobs own the data plane ({@code stub-stage-worker=false}), poll Job status to:
 *
 * <ul>
 *   <li>emit latency / heartbeat / completeness-related metrics (same series as StubStageWorker)
 *   <li>mark the execution {@code failed} on BackoffLimitExceeded / Failed
 *   <li>mark the execution {@code failed} early on ImagePullBackOff / ErrImagePull (stuck Pending)
 *   <li>spawn the next stage Job after the current stage Completes
 *   <li>mark the execution {@code completed} after the final stage Completes
 * </ul>
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "pipeline.k8s", name = "enabled", havingValue = "true")
public class PipeletJobStatusPoller {

  private static final Logger log = LoggerFactory.getLogger(PipeletJobStatusPoller.class);

  private static final Pattern RECORDS_PATTERN =
      Pattern.compile(
          "(?:emitted records=|recordCount[\"'\\s:=]+|items=|kept[=:\\s]+)(\\d+)",
          Pattern.CASE_INSENSITIVE);

  private final KubernetesClient kubernetesClient;
  private final PipelineExecutionRepository executionRepository;
  private final PipelineRepository pipelineRepository;
  private final PipelineStepRepository stepRepository;
  private final PipelineRunOrchestrator orchestrator;
  private final PipeletJobClient pipeletJobClient;
  private final PipeletJobRequestFactory jobRequestFactory;
  private final PipeletAmqpUrlFactory amqpUrlFactory;
  private final PipelineOrchestrationProperties orchestrationProperties;
  private final PipeletMetricsEmitter metricsEmitter;
  private final PipelineLogEmitter logEmitter;

  /** Stages already published to Micrometer / log indexer for this JVM. */
  private final Set<String> observedStages = ConcurrentHashMap.newKeySet();

  public PipeletJobStatusPoller(
      KubernetesClient kubernetesClient,
      PipelineExecutionRepository executionRepository,
      PipelineRepository pipelineRepository,
      PipelineStepRepository stepRepository,
      PipelineRunOrchestrator orchestrator,
      PipeletJobClient pipeletJobClient,
      PipeletJobRequestFactory jobRequestFactory,
      PipeletAmqpUrlFactory amqpUrlFactory,
      PipelineOrchestrationProperties orchestrationProperties,
      PipeletMetricsEmitter metricsEmitter,
      PipelineLogEmitter logEmitter) {
    this.kubernetesClient = kubernetesClient;
    this.executionRepository = executionRepository;
    this.pipelineRepository = pipelineRepository;
    this.stepRepository = stepRepository;
    this.orchestrator = orchestrator;
    this.pipeletJobClient = pipeletJobClient;
    this.jobRequestFactory = jobRequestFactory;
    this.amqpUrlFactory = amqpUrlFactory;
    this.orchestrationProperties = orchestrationProperties;
    this.metricsEmitter = metricsEmitter;
    this.logEmitter = logEmitter;
    log.info("Pipelet Job status poller enabled");
  }

  @Scheduled(fixedDelayString = "${pipeline.k8s.job-status-poll-interval-ms:2000}")
  @Transactional
  public void poll() {
    if (orchestrationProperties.isStubStageWorker()) {
      return;
    }
    List<PipelineExecution> running =
        executionRepository.findByStatus(ExecutionStatus.RUNNING);
    for (PipelineExecution execution : running) {
      try {
        reconcile(execution);
      } catch (Exception ex) {
        log.warn(
            "Job status poll failed for execution {}: {}",
            execution.getId(),
            ex.getMessage());
      }
    }
  }

  private void reconcile(PipelineExecution execution) {
    String namespace = PipeletK8sProperties.namespaceForTenant(execution.getTenantId());
    List<Job> jobs =
        kubernetesClient
            .batch()
            .v1()
            .jobs()
            .inNamespace(namespace)
            .withLabel(PipeletK8sLabels.EXECUTION_ID, execution.getId())
            .list()
            .getItems();

    if (jobs.isEmpty()) {
      return;
    }

    for (Job job : jobs) {
      if (isFailed(job)) {
        observeFailedStage(execution, job, namespace);
        String reason = failureReason(job);
        String detail = enrichWithPodDiagnostics(namespace, execution.getId(), job, reason);
        orchestrator.markFailed(
            execution.getId(),
            "Kubernetes Job failed: "
                + job.getMetadata().getName()
                + (detail == null || detail.isBlank() ? "" : " — " + detail));
        return;
      }

      Optional<String> stuck = diagnoseStuckPods(namespace, execution.getId(), job);
      if (stuck.isPresent()) {
        observeFailedStage(execution, job, namespace, stuck.get());
        log.warn(
            "Failing execution {} — stuck pod/image issue on {}: {}",
            execution.getId(),
            job.getMetadata().getName(),
            stuck.get());
        orchestrator.markFailed(
            execution.getId(),
            "Kubernetes Job stuck: " + job.getMetadata().getName() + " — " + stuck.get());
        return;
      }
    }

    List<PipelineStep> steps =
        stepRepository.findByPipelineIdOrdered(execution.getPipelineId());
    int stageCount = steps.size();
    if (stageCount == 0) {
      orchestrator.markFailed(execution.getId(), "Pipeline has no steps");
      return;
    }

    long bestRecordsOut = 0L;
    int highestComplete = 0;
    for (Job job : jobs) {
      if (!isComplete(job)) {
        continue;
      }
      Integer stage = stageOrder(job);
      if (stage == null) {
        continue;
      }
      highestComplete = Math.max(highestComplete, stage);
      long records = observeCompletedStage(execution, job, namespace, steps);
      bestRecordsOut = Math.max(bestRecordsOut, records);
    }

    if (highestComplete <= 0) {
      return;
    }

    if (highestComplete >= stageCount) {
      long records = Math.max(1L, bestRecordsOut);
      orchestrator.markCompleted(execution.getId(), records, records);
      return;
    }

    int nextStage = highestComplete + 1;
    boolean nextExists =
        jobs.stream().anyMatch(j -> Objects.equals(stageOrder(j), nextStage));
    if (nextExists) {
      return;
    }

    Pipeline pipeline =
        pipelineRepository
            .findById(execution.getPipelineId())
            .filter(p -> execution.getTenantId().equals(p.getTenantId()))
            .orElse(null);
    if (pipeline == null) {
      orchestrator.markFailed(execution.getId(), "Pipeline not found for execution");
      return;
    }
    PipelineStep nextStep =
        steps.stream().filter(s -> s.getStepOrder() == nextStage).findFirst().orElse(null);
    if (nextStep == null) {
      orchestrator.markFailed(
          execution.getId(), "Missing pipeline step for stage " + nextStage);
      return;
    }

    String ioMode = PipelineIoMode.fromExecutionConfigJson(pipeline.getExecutionConfig());
    String amqpUrl = amqpUrlFactory.resolve();
    log.info(
        "Advancing execution {} to stage {}/{} (pipelet={})",
        execution.getId(),
        nextStage,
        stageCount,
        nextStep.getPipeletId());
    pipeletJobClient.create(
        jobRequestFactory.build(
            pipeline, nextStep, execution.getId(), stageCount, ioMode, amqpUrl));
  }

  private long observeCompletedStage(
      PipelineExecution execution, Job job, String namespace, List<PipelineStep> steps) {
    Integer stage = stageOrder(job);
    if (stage == null) {
      return 0L;
    }
    String key = execution.getId() + ":" + stage;
    if (!observedStages.add(key)) {
      return parseRecordsFromLogs(namespace, execution.getId(), stage);
    }

    String pipeletId = pipeletId(job, steps, stage);
    Duration duration = jobDuration(job);
    long records = parseRecordsFromLogs(namespace, execution.getId(), stage);
    if (records <= 0) {
      records = 1L;
    }

    metricsEmitter.recordBatch(
        execution.getTenantId(),
        execution.getPipelineId(),
        pipeletId,
        records,
        records,
        duration.isZero() ? Duration.ofMillis(1) : duration);
    metricsEmitter.touchHeartbeat(
        execution.getTenantId(), execution.getPipelineId(), pipeletId);
    logEmitter.emitStageProcessed(
        execution.getTenantId(),
        execution.getPipelineId(),
        execution.getId(),
        pipeletId,
        records,
        records,
        Math.max(1L, duration.toMillis()));
    return records;
  }

  private void observeFailedStage(PipelineExecution execution, Job job, String namespace) {
    observeFailedStage(execution, job, namespace, failureReason(job));
  }

  private void observeFailedStage(
      PipelineExecution execution, Job job, String namespace, String reasonOverride) {
    Integer stage = stageOrder(job);
    String key = execution.getId() + ":fail:" + (stage == null ? "unknown" : stage);
    if (!observedStages.add(key)) {
      return;
    }
    String pipeletId =
        job.getMetadata() != null && job.getMetadata().getLabels() != null
            ? job.getMetadata().getLabels().getOrDefault(PipeletK8sLabels.PIPELET_ID, "unknown")
            : "unknown";
    PipeletErrorType errorType =
        reasonOverride != null && PipeletPodDiagnostics.isImagePullFailure(extractReasonToken(reasonOverride))
            ? PipeletErrorType.IMAGE_PULL
            : PipeletErrorType.PROCESSING;
    // Also classify when message contains ImagePullBackOff text without exact token match.
    if (errorType == PipeletErrorType.PROCESSING
        && reasonOverride != null
        && reasonOverride.toLowerCase().contains("image pull")) {
      errorType = PipeletErrorType.IMAGE_PULL;
    }
    metricsEmitter.recordCriticalError(
        execution.getTenantId(),
        execution.getPipelineId(),
        pipeletId,
        errorType);
    metricsEmitter.touchHeartbeat(
        execution.getTenantId(), execution.getPipelineId(), pipeletId);
    String reason =
        reasonOverride == null || reasonOverride.isBlank()
            ? failureReason(job)
            : reasonOverride;
    logEmitter.emit(
        com.dashflow.api.observability.PipelineLogDocument.error(
            execution.getTenantId(),
            execution.getPipelineId(),
            execution.getId(),
            pipeletId,
            reason == null ? "Kubernetes Job failed" : reason));
  }

  private Optional<String> diagnoseStuckPods(
      String namespace, String executionId, Job job) {
    Integer stage = stageOrder(job);
    try {
      var podOp = kubernetesClient.pods().inNamespace(namespace);
      List<Pod> pods;
      if (stage != null) {
        pods =
            podOp
                .withLabel(PipeletK8sLabels.EXECUTION_ID, executionId)
                .withLabel(PipeletK8sLabels.STAGE_ORDER, String.valueOf(stage))
                .list()
                .getItems();
      } else if (job.getMetadata() != null && job.getMetadata().getName() != null) {
        pods =
            podOp.withLabel("job-name", job.getMetadata().getName()).list().getItems();
      } else {
        return Optional.empty();
      }
      return PipeletPodDiagnostics.diagnose(pods);
    } catch (Exception ex) {
      log.debug(
          "Could not diagnose pods for execution {} job {}: {}",
          executionId,
          job.getMetadata() != null ? job.getMetadata().getName() : "?",
          ex.getMessage());
      return Optional.empty();
    }
  }

  private String enrichWithPodDiagnostics(
      String namespace, String executionId, Job job, String jobReason) {
    Optional<String> podReason = diagnoseStuckPods(namespace, executionId, job);
    if (podReason.isPresent()) {
      if (jobReason == null || jobReason.isBlank()) {
        return podReason.get();
      }
      return jobReason + "; " + podReason.get();
    }
    return jobReason;
  }

  private static String extractReasonToken(String detail) {
    if (detail == null || detail.isBlank()) {
      return "";
    }
    // Prefer known tokens embedded in diagnostic strings.
    String lower = detail.toLowerCase();
    for (String token :
        List.of(
            "ImagePullBackOff",
            "ErrImagePull",
            "InvalidImageName",
            "CrashLoopBackOff",
            "CreateContainerConfigError")) {
      if (lower.contains(token.toLowerCase())) {
        return token;
      }
    }
    return detail;
  }

  private long parseRecordsFromLogs(String namespace, String executionId, int stage) {
    try {
      List<Pod> pods =
          kubernetesClient
              .pods()
              .inNamespace(namespace)
              .withLabel(PipeletK8sLabels.EXECUTION_ID, executionId)
              .withLabel(PipeletK8sLabels.STAGE_ORDER, String.valueOf(stage))
              .list()
              .getItems();
      long best = 0L;
      for (Pod pod : pods) {
        if (pod.getMetadata() == null || pod.getMetadata().getName() == null) {
          continue;
        }
        String logs =
            kubernetesClient
                .pods()
                .inNamespace(namespace)
                .withName(pod.getMetadata().getName())
                .getLog();
        if (logs == null || logs.isBlank()) {
          continue;
        }
        Matcher matcher = RECORDS_PATTERN.matcher(logs);
        while (matcher.find()) {
          try {
            best = Math.max(best, Long.parseLong(matcher.group(1)));
          } catch (NumberFormatException ignored) {
            // continue
          }
        }
      }
      return best;
    } catch (Exception ex) {
      log.debug(
          "Could not parse records from logs for execution {} stage {}: {}",
          executionId,
          stage,
          ex.getMessage());
      return 0L;
    }
  }

  private static String pipeletId(Job job, List<PipelineStep> steps, int stage) {
    if (job.getMetadata() != null && job.getMetadata().getLabels() != null) {
      String fromLabel = job.getMetadata().getLabels().get(PipeletK8sLabels.PIPELET_ID);
      if (fromLabel != null && !fromLabel.isBlank()) {
        return fromLabel;
      }
    }
    return steps.stream()
        .filter(s -> s.getStepOrder() == stage)
        .map(PipelineStep::getPipeletId)
        .findFirst()
        .orElse("unknown");
  }

  private static Duration jobDuration(Job job) {
    JobStatus status = job.getStatus();
    if (status == null || status.getStartTime() == null || status.getCompletionTime() == null) {
      return Duration.ofMillis(1);
    }
    try {
      Instant start = Instant.parse(status.getStartTime());
      Instant end = Instant.parse(status.getCompletionTime());
      Duration d = Duration.between(start, end);
      return d.isNegative() || d.isZero() ? Duration.ofMillis(1) : d;
    } catch (Exception ex) {
      return Duration.ofMillis(1);
    }
  }

  private boolean isFailed(Job job) {
    JobStatus status = job.getStatus();
    if (status == null) {
      return false;
    }
    if (status.getFailed() != null && status.getFailed() > 0) {
      if (status.getConditions() != null) {
        for (JobCondition c : status.getConditions()) {
          if ("Failed".equalsIgnoreCase(c.getType()) && "True".equalsIgnoreCase(c.getStatus())) {
            return true;
          }
        }
      }
      Integer backoff = job.getSpec() != null ? job.getSpec().getBackoffLimit() : null;
      int limit = backoff == null ? 6 : backoff;
      return status.getFailed() > limit;
    }
    return false;
  }

  private boolean isComplete(Job job) {
    JobStatus status = job.getStatus();
    if (status == null) {
      return false;
    }
    if (status.getSucceeded() != null && status.getSucceeded() > 0) {
      return true;
    }
    if (status.getConditions() != null) {
      for (JobCondition c : status.getConditions()) {
        if ("Complete".equalsIgnoreCase(c.getType()) && "True".equalsIgnoreCase(c.getStatus())) {
          return true;
        }
      }
    }
    return false;
  }

  private String failureReason(Job job) {
    JobStatus status = job.getStatus();
    if (status == null || status.getConditions() == null) {
      return null;
    }
    for (JobCondition c : status.getConditions()) {
      if ("Failed".equalsIgnoreCase(c.getType()) && "True".equalsIgnoreCase(c.getStatus())) {
        if (c.getMessage() != null && !c.getMessage().isBlank()) {
          return c.getMessage();
        }
        return c.getReason();
      }
    }
    return null;
  }

  private Integer stageOrder(Job job) {
    if (job.getMetadata() == null || job.getMetadata().getLabels() == null) {
      return null;
    }
    String raw = job.getMetadata().getLabels().get(PipeletK8sLabels.STAGE_ORDER);
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
