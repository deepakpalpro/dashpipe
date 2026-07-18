package com.dashpipe.api.pipeline;

import com.dashpipe.api.k8s.PipeletJobClient;
import com.dashpipe.api.k8s.PipeletJobRequestFactory;
import com.dashpipe.api.messaging.QueueNaming;
import com.dashpipe.api.messaging.RabbitMessagingConfig;
import com.dashpipe.api.observability.PipelineLogEmitter;
import com.dashpipe.api.observability.PipeletMetricsEmitter;
import com.dashpipe.api.usage.MeterAgent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Stub stage worker for Wave 2: advances stages over RabbitMQ. Spawns subsequent stage Jobs via
 * {@link PipeletJobClient} (stage 1 is spawned by the orchestrator). Emits pipelet metrics (W4-US01),
 * structured pipeline logs (W4-US04), and billable usage via {@link MeterAgent} (W5-US02).
 *
 * <p>Enabled when messages are published to {@link RabbitMessagingConfig#STUB_STAGE_WORKER_QUEUE}
 * ({@code pipeline.orchestration.stub-stage-worker=true}).
 */
@Component
public class StubStageWorker {

  /** Stub processes one logical record per stage message (fixture-friendly for completeness). */
  public static final long STUB_RECORDS_PER_STAGE = 1L;

  private static final Logger log = LoggerFactory.getLogger(StubStageWorker.class);

  private final RabbitTemplate rabbitTemplate;
  private final PipelineRunOrchestrator orchestrator;
  private final PipeletJobClient pipeletJobClient;
  private final PipelineStepRepository pipelineStepRepository;
  private final PipelineRepository pipelineRepository;
  private final PipeletMetricsEmitter pipeletMetricsEmitter;
  private final PipelineLogEmitter pipelineLogEmitter;
  private final MeterAgent meterAgent;
  private final PipelineOrchestrationProperties orchestrationProperties;
  private final PipeletJobRequestFactory jobRequestFactory;
  private final PipelineExecutionStepTracker stepTracker;

  public StubStageWorker(
      RabbitTemplate rabbitTemplate,
      PipelineRunOrchestrator orchestrator,
      PipeletJobClient pipeletJobClient,
      PipelineStepRepository pipelineStepRepository,
      PipelineRepository pipelineRepository,
      PipeletMetricsEmitter pipeletMetricsEmitter,
      PipelineLogEmitter pipelineLogEmitter,
      MeterAgent meterAgent,
      PipelineOrchestrationProperties orchestrationProperties,
      PipeletJobRequestFactory jobRequestFactory,
      PipelineExecutionStepTracker stepTracker) {
    this.rabbitTemplate = rabbitTemplate;
    this.orchestrator = orchestrator;
    this.pipeletJobClient = pipeletJobClient;
    this.pipelineStepRepository = pipelineStepRepository;
    this.pipelineRepository = pipelineRepository;
    this.pipeletMetricsEmitter = pipeletMetricsEmitter;
    this.pipelineLogEmitter = pipelineLogEmitter;
    this.meterAgent = meterAgent;
    this.orchestrationProperties = orchestrationProperties;
    this.jobRequestFactory = jobRequestFactory;
    this.stepTracker = stepTracker;
  }

  @RabbitListener(queues = RabbitMessagingConfig.STUB_STAGE_WORKER_QUEUE)
  public void onStageMessage(StageMessage message) {
    if (message == null) {
      return;
    }
    if (!orchestrationProperties.isStubStageWorker()) {
      log.debug("Ignoring stub stage message (stub-stage-worker=false)");
      return;
    }
    log.debug(
        "Stub stage {}/{} for execution {} ioMode={}",
        message.stageOrder(),
        message.stageCount(),
        message.executionId(),
        message.ioMode());

    Instant stageStarted = Instant.now();
    stepTracker.markRunning(message.executionId(), message.stageOrder(), stageStarted);

    long startedNanos = System.nanoTime();
    Duration processing = Duration.ofNanos(Math.max(1L, System.nanoTime() - startedNanos));
    pipeletMetricsEmitter.recordBatch(
        message.tenantId(),
        message.pipelineId(),
        message.pipeletId(),
        STUB_RECORDS_PER_STAGE,
        STUB_RECORDS_PER_STAGE,
        processing);
    pipeletMetricsEmitter.touchHeartbeat(
        message.tenantId(), message.pipelineId(), message.pipeletId());
    pipelineLogEmitter.emitStageProcessed(
        message.tenantId(),
        message.pipelineId(),
        message.executionId(),
        message.pipeletId(),
        STUB_RECORDS_PER_STAGE,
        STUB_RECORDS_PER_STAGE,
        Math.max(1L, processing.toMillis()));
    meterAgent.recordStageProcessed(
        message.tenantId(),
        message.pipelineId(),
        message.executionId(),
        message.pipeletId(),
        message.stageOrder(),
        message.stageCount(),
        STUB_RECORDS_PER_STAGE,
        processing);

    String exchange = QueueNaming.pipelineExchange(message.tenantId(), message.pipelineId());
    String ioMode = PipelineIoMode.normalize(message.ioMode());
    String amqpUrl = message.amqpUrl();

    stepTracker.markCompleted(
        message.executionId(),
        message.stageOrder(),
        STUB_RECORDS_PER_STAGE,
        STUB_RECORDS_PER_STAGE,
        stageStarted,
        Instant.now());

    if (message.stageOrder() >= message.stageCount()) {
      orchestrator.markCompleted(message.executionId(), 1, 1);
      return;
    }

    int next = message.stageOrder() + 1;
    stepTracker.markRunning(message.executionId(), next, Instant.now());
    List<PipelineStep> steps = pipelineStepRepository.findByPipelineIdOrdered(message.pipelineId());
    PipelineStep nextStep =
        steps.stream().filter(s -> s.getStepOrder() == next).findFirst().orElse(null);
    String nextPipeletId = nextStep != null ? nextStep.getPipeletId() : "unknown";

    Pipeline pipeline =
        pipelineRepository.findById(message.pipelineId()).orElse(null);
    if (pipeline != null && nextStep != null) {
      pipeletJobClient.create(
          jobRequestFactory.build(
              pipeline, nextStep, message.executionId(), message.stageCount(), ioMode, amqpUrl));
    }

    StageMessage nextMessage =
        new StageMessage(
            message.executionId(),
            message.pipelineId(),
            message.tenantId(),
            nextPipeletId,
            next,
            message.stageCount(),
            message.payload(),
            ioMode,
            amqpUrl);
    rabbitTemplate.convertAndSend(exchange, QueueNaming.stageRoutingKey(next), nextMessage);
    rabbitTemplate.convertAndSend(RabbitMessagingConfig.STUB_STAGE_WORKER_QUEUE, nextMessage);
  }
}
