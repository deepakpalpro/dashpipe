package com.dashpipe.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dashpipe.api.k8s.PipeletJobClient;
import com.dashpipe.api.k8s.PipeletJobEnv;
import com.dashpipe.api.k8s.PipeletJobHandle;
import com.dashpipe.api.k8s.PipeletJobRequest;
import com.dashpipe.api.k8s.PipeletJobRequestFactory;
import com.dashpipe.api.messaging.PipelineStageTopology;
import com.dashpipe.api.messaging.PipelineTopology;
import com.dashpipe.api.messaging.PipelineTopologyService;
import com.dashpipe.api.messaging.RabbitMessagingConfig;
import com.dashpipe.api.observability.CompletenessMetricsPublisher;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PipelineRunOrchestratorTest {

  @Mock private PipelineExecutionRepository executionRepository;
  @Mock private PipelineTopologyService topologyService;
  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private PipeletJobClient pipeletJobClient;
  @Mock private CompletenessMetricsPublisher completenessMetricsPublisher;
  @Mock private PipeletAmqpUrlFactory amqpUrlFactory;
  @Mock private PipeletJobRequestFactory jobRequestFactory;
  @Mock private PipelineExecutionStepTracker stepTracker;

  private PipelineOrchestrationProperties orchestrationProperties;
  private PipelineRunOrchestrator orchestrator;

  @BeforeEach
  void setUp() {
    orchestrationProperties = new PipelineOrchestrationProperties();
    orchestrationProperties.setStubStageWorker(true);
    when(amqpUrlFactory.resolve()).thenReturn("amqp://pipeline:pipeline@localhost:5672/");
    when(jobRequestFactory.build(any(), any(), anyString(), anyInt(), anyString(), anyString()))
        .thenAnswer(
            inv -> {
              Pipeline pipeline = inv.getArgument(0);
              PipelineStep step = inv.getArgument(1);
              String executionId = inv.getArgument(2);
              int stageCount = inv.getArgument(3);
              String ioMode = inv.getArgument(4);
              String amqpUrl = inv.getArgument(5);
              return PipeletJobRequest.of(
                      pipeline.getTenantId(),
                      pipeline.getId(),
                      executionId,
                      step.getPipeletId(),
                      step.getStepOrder(),
                      stageCount,
                      "q-in",
                      stageCount > step.getStepOrder() ? "q-out" : null,
                      ioMode,
                      amqpUrl)
                  .withEnv(PipeletJobEnv.empty());
            });
    orchestrator =
        new PipelineRunOrchestrator(
            executionRepository,
            topologyService,
            rabbitTemplate,
            pipeletJobClient,
            completenessMetricsPublisher,
            orchestrationProperties,
            amqpUrlFactory,
            jobRequestFactory,
            stepTracker);
    when(executionRepository.save(any(PipelineExecution.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(topologyService.declare(anyString(), anyString(), anyInt()))
        .thenAnswer(
            inv ->
                new PipelineTopology(
                    inv.getArgument(0),
                    inv.getArgument(1),
                    "tenant." + inv.getArgument(0) + ".pipeline." + inv.getArgument(1),
                    List.of(
                        new PipelineStageTopology(1, "q1", "q2", "d1", "stage.1"),
                        new PipelineStageTopology(2, "q2", null, "d2", "stage.2"))));
    when(pipeletJobClient.create(any(PipeletJobRequest.class)))
        .thenAnswer(inv -> PipeletJobHandle.stubbed(inv.getArgument(0)));
  }

  @Test
  void start_createsExecution() {
    Pipeline pipeline = activePipeline();
    List<PipelineStep> steps = List.of(step(1, "plet-a"), step(2, "plet-b"));

    PipelineExecution execution =
        orchestrator.start(pipeline, steps, ExecutionTrigger.MANUAL);

    assertThat(execution.getId()).isNotBlank();
    assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(execution.getPipelineId()).isEqualTo("p1");
    assertThat(execution.getTenantId()).isEqualTo("tenant-a");
    assertThat(execution.getTrigger()).isEqualTo(ExecutionTrigger.MANUAL);

    verify(topologyService).declare("tenant-a", "p1", 2);
    verify(topologyService).purgeStageQueues(any(PipelineTopology.class));
    verify(rabbitTemplate)
        .convertAndSend(eq(RabbitMessagingConfig.STUB_STAGE_WORKER_QUEUE), any(StageMessage.class));
    verify(executionRepository, org.mockito.Mockito.atLeast(2)).save(any(PipelineExecution.class));

    ArgumentCaptor<PipeletJobRequest> jobCaptor = ArgumentCaptor.forClass(PipeletJobRequest.class);
    verify(pipeletJobClient).create(jobCaptor.capture());
    PipeletJobRequest job = jobCaptor.getValue();
    assertThat(job.tenantId()).isEqualTo("tenant-a");
    assertThat(job.pipelineId()).isEqualTo("p1");
    assertThat(job.executionId()).isEqualTo(execution.getId());
    assertThat(job.pipeletId()).isEqualTo("plet-a");
    assertThat(job.stageOrder()).isEqualTo(1);
    assertThat(job.jobName()).isEqualTo("exec-" + execution.getId() + "-stage-1");
    assertThat(job.namespace()).isEqualTo("tenant-tenant-a");
    assertThat(job.ioMode()).isEqualTo(PipelineIoMode.QUEUE);
    assertThat(job.amqpUrl()).isEqualTo("amqp://pipeline:pipeline@localhost:5672/");
    assertThat(job.triggerPayload()).isNull();
    verify(jobRequestFactory)
        .build(eq(pipeline), eq(steps.get(0)), eq(execution.getId()), eq(2), anyString(), anyString());
  }

  @Test
  void start_injectsTriggerPayloadOntoFirstStageJob() {
    Pipeline pipeline = activePipeline();
    List<PipelineStep> steps = List.of(step(1, "plet-manual-source"));
    com.fasterxml.jackson.databind.JsonNode payload =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .createObjectNode()
            .put("sku", "food-01")
            .put("qty", 3);

    PipelineExecution execution =
        orchestrator.start(pipeline, steps, ExecutionTrigger.MANUAL, payload);

    ArgumentCaptor<PipeletJobRequest> jobCaptor = ArgumentCaptor.forClass(PipeletJobRequest.class);
    verify(pipeletJobClient).create(jobCaptor.capture());
    assertThat(jobCaptor.getValue().triggerPayload()).isEqualTo(payload.toString());
    assertThat(execution.getTrigger()).isEqualTo(ExecutionTrigger.MANUAL);
  }

  @Test
  void start_passesStdioIoModeFromExecutionConfig() {
    Pipeline pipeline = activePipeline();
    pipeline.setExecutionConfig("{\"ioMode\":\"stdio\",\"batchSize\":\"10\"}");
    List<PipelineStep> steps = List.of(step(1, "plet-a"));

    orchestrator.start(pipeline, steps, ExecutionTrigger.MANUAL);

    ArgumentCaptor<PipeletJobRequest> jobCaptor = ArgumentCaptor.forClass(PipeletJobRequest.class);
    verify(pipeletJobClient).create(jobCaptor.capture());
    assertThat(jobCaptor.getValue().ioMode()).isEqualTo(PipelineIoMode.STDIO);
  }

  @Test
  void start_skipsStubWorkerWhenDisabled() {
    orchestrationProperties.setStubStageWorker(false);
    Pipeline pipeline = activePipeline();
    List<PipelineStep> steps = List.of(step(1, "plet-a"));

    orchestrator.start(pipeline, steps, ExecutionTrigger.MANUAL);

    verify(topologyService).purgeStageQueues(any(PipelineTopology.class));
    verify(rabbitTemplate, never())
        .convertAndSend(eq(RabbitMessagingConfig.STUB_STAGE_WORKER_QUEUE), any(StageMessage.class));
    // Queue-mode K8s runs do not publish stage-1 kickoff (sources use SOURCE_TRIGGER=once).
    verify(rabbitTemplate, never())
        .convertAndSend(anyString(), anyString(), any(StageMessage.class));
  }

  @Test
  void start_rejectsArchived() {
    Pipeline pipeline = activePipeline();
    pipeline.setStatus(PipelineStatus.ARCHIVED);

    assertThatThrownBy(
            () ->
                orchestrator.start(
                    pipeline, List.of(step(1, "plet")), ExecutionTrigger.MANUAL))
        .isInstanceOf(PipelineValidationException.class)
        .hasMessageContaining("archived");

    verify(executionRepository, never()).save(any());
    verify(pipeletJobClient, never()).create(any());
  }

  @Test
  void start_rejectsDraft() {
    Pipeline pipeline = activePipeline();
    pipeline.setStatus(PipelineStatus.DRAFT);

    assertThatThrownBy(
            () ->
                orchestrator.start(
                    pipeline, List.of(step(1, "plet")), ExecutionTrigger.MANUAL))
        .isInstanceOf(PipelineValidationException.class)
        .hasMessageContaining("active");
  }

  @Test
  void start_rejectsEmptySteps() {
    assertThatThrownBy(
            () -> orchestrator.start(activePipeline(), List.of(), ExecutionTrigger.MANUAL))
        .isInstanceOf(PipelineValidationException.class)
        .hasMessageContaining("steps");
  }

  @Test
  void markCompleted_persistsCompletenessAndPublishesGauge() {
    PipelineExecution execution = new PipelineExecution();
    execution.setId("exec-1");
    execution.setTenantId("tenant-a");
    execution.setPipelineId("p1");
    execution.setStatus(ExecutionStatus.RUNNING);
    when(executionRepository.findById("exec-1")).thenReturn(java.util.Optional.of(execution));

    orchestrator.markCompleted("exec-1", 100, 98);

    assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
    assertThat(execution.getCompletenessPct()).isEqualByComparingTo(new BigDecimal("98.00"));
    assertThat(execution.getRecordsIn()).isEqualTo(100);
    assertThat(execution.getRecordsOut()).isEqualTo(98);
    verify(completenessMetricsPublisher).publish("tenant-a", "p1", 0.98);
    verify(executionRepository).save(execution);
  }

  @Test
  void cancel_marksCancelledDeletesJobsAndPurgesQueues() {
    PipelineExecution execution = new PipelineExecution();
    execution.setId("exec-1");
    execution.setTenantId("tenant-a");
    execution.setPipelineId("p1");
    execution.setStatus(ExecutionStatus.RUNNING);

    PipelineExecution cancelled = orchestrator.cancel(execution, 2);

    assertThat(cancelled.getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
    assertThat(cancelled.getCompletedAt()).isNotNull();
    assertThat(cancelled.getErrorSummary()).contains("Force stopped by user");
    verify(pipeletJobClient).deleteByExecution("tenant-a", "exec-1");
    verify(topologyService).declare("tenant-a", "p1", 2);
    verify(topologyService).purgeStageQueues(any(PipelineTopology.class));
    verify(executionRepository).save(execution);
  }

  @Test
  void cancel_isIdempotentWhenAlreadyTerminal() {
    PipelineExecution execution = new PipelineExecution();
    execution.setId("exec-1");
    execution.setTenantId("tenant-a");
    execution.setPipelineId("p1");
    execution.setStatus(ExecutionStatus.FAILED);

    PipelineExecution result = orchestrator.cancel(execution, 1);

    assertThat(result.getStatus()).isEqualTo(ExecutionStatus.FAILED);
    verify(pipeletJobClient, never()).deleteByExecution(anyString(), anyString());
    verify(topologyService, never()).purgeStageQueues(any());
  }

  private static Pipeline activePipeline() {
    Pipeline pipeline = new Pipeline();
    pipeline.setId("p1");
    pipeline.setTenantId("tenant-a");
    pipeline.setName("pipe");
    pipeline.setVersion(3);
    pipeline.setStatus(PipelineStatus.ACTIVE);
    pipeline.setVisibility(PipelineVisibility.PRIVATE);
    pipeline.setExecutionMode(PipelineExecutionMode.ASYNC);
    return pipeline;
  }

  private static PipelineStep step(int order, String pipeletId) {
    PipelineStep step = new PipelineStep();
    step.setId("s" + order);
    step.setPipelineId("p1");
    step.setPipeletId(pipeletId);
    step.setStepOrder(order);
    return step;
  }
}
