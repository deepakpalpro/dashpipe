package com.dashflow.api.k8s;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dashflow.api.observability.PipeletMetricsEmitter;
import com.dashflow.api.observability.PipelineLogEmitter;
import com.dashflow.api.pipeline.ExecutionStatus;
import com.dashflow.api.pipeline.Pipeline;
import com.dashflow.api.pipeline.PipelineExecution;
import com.dashflow.api.pipeline.PipelineExecutionRepository;
import com.dashflow.api.pipeline.PipelineExecutionStepTracker;
import com.dashflow.api.pipeline.PipelineOrchestrationProperties;
import com.dashflow.api.pipeline.PipelineRepository;
import com.dashflow.api.pipeline.PipelineRunOrchestrator;
import com.dashflow.api.pipeline.PipelineStep;
import com.dashflow.api.pipeline.PipelineStepRepository;
import com.dashflow.api.pipeline.PipeletAmqpUrlFactory;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.BatchAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.V1BatchAPIGroupDSL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PipeletJobStatusPollerTest {

  @Mock private KubernetesClient kubernetesClient;
  @Mock private PipelineExecutionRepository executionRepository;
  @Mock private PipelineRepository pipelineRepository;
  @Mock private PipelineStepRepository stepRepository;
  @Mock private PipelineRunOrchestrator orchestrator;
  @Mock private PipeletJobClient pipeletJobClient;
  @Mock private PipeletJobRequestFactory jobRequestFactory;
  @Mock private PipeletAmqpUrlFactory amqpUrlFactory;
  @Mock private PipeletMetricsEmitter metricsEmitter;
  @Mock private PipelineLogEmitter logEmitter;
  @Mock private PipelineExecutionStepTracker stepTracker;
  @Mock private BatchAPIGroupDSL batch;
  @Mock private V1BatchAPIGroupDSL v1;
  @Mock private MixedOperation jobsOp;
  @Mock private NonNamespaceOperation nsOp;
  @Mock private FilterWatchListDeletable labeledOp;
  @Mock private MixedOperation podsOp;
  @Mock private NonNamespaceOperation podsNsOp;
  @Mock private FilterWatchListDeletable podsLabeledOp;

  private PipelineOrchestrationProperties orchestrationProperties;
  private PipeletJobStatusPoller poller;

  @BeforeEach
  @SuppressWarnings({"rawtypes", "unchecked"})
  void setUp() {
    orchestrationProperties = new PipelineOrchestrationProperties();
    orchestrationProperties.setStubStageWorker(false);
    when(amqpUrlFactory.resolve())
        .thenReturn("amqp://pipeline:pipeline@host.docker.internal:5672/");
    when(kubernetesClient.batch()).thenReturn(batch);
    when(batch.v1()).thenReturn(v1);
    when(v1.jobs()).thenReturn(jobsOp);
    doReturn(nsOp).when(jobsOp).inNamespace(anyString());
    doReturn(labeledOp).when(nsOp).withLabel(anyString(), anyString());

    poller =
        new PipeletJobStatusPoller(
            kubernetesClient,
            executionRepository,
            pipelineRepository,
            stepRepository,
            orchestrator,
            pipeletJobClient,
            jobRequestFactory,
            amqpUrlFactory,
            orchestrationProperties,
            metricsEmitter,
            logEmitter,
            stepTracker);
  }

  @Test
  void marksFailedOnBackoffLimitExceeded() {
    PipelineExecution execution = runningExecution();
    when(executionRepository.findByStatus(ExecutionStatus.RUNNING)).thenReturn(List.of(execution));

    Job failed = job(1, false, true, "BackoffLimitExceeded");
    JobList list = new JobList();
    list.setItems(List.of(failed));
    when(labeledOp.list()).thenReturn(list);

    poller.poll();

    verify(orchestrator).markFailed(eq("exec-1"), contains("BackoffLimitExceeded"));
    verify(pipeletJobClient, never()).create(any());
  }

  @Test
  void advancesToNextStageOnComplete() {
    PipelineExecution execution = runningExecution();
    when(executionRepository.findByStatus(ExecutionStatus.RUNNING)).thenReturn(List.of(execution));

    Job complete = job(1, true, false, null);
    JobList list = new JobList();
    list.setItems(List.of(complete));
    when(labeledOp.list()).thenReturn(list);

    Pipeline pipeline = new Pipeline();
    pipeline.setId("p1");
    pipeline.setTenantId("T001");
    pipeline.setExecutionConfig("{\"ioMode\":\"queue\"}");
    when(pipelineRepository.findById("p1")).thenReturn(Optional.of(pipeline));

    PipelineStep s1 = step(1, "plet-a");
    PipelineStep s2 = step(2, "plet-b");
    when(stepRepository.findByPipelineIdOrdered("p1")).thenReturn(List.of(s1, s2));

    PipeletJobRequest nextReq =
        PipeletJobRequest.of(
            "T001", "p1", "exec-1", "plet-b", 2, 2, "qin", null, "queue", "amqp://x");
    when(jobRequestFactory.build(
            eq(pipeline), eq(s2), eq("exec-1"), eq(2), anyString(), anyString()))
        .thenReturn(nextReq);

    poller.poll();

    verify(pipeletJobClient).create(nextReq);
    verify(orchestrator, never()).markCompleted(anyString(), anyInt(), anyInt());
    verify(orchestrator, never()).markFailed(anyString(), anyString());
  }

  @Test
  void marksCompletedWhenFinalStageSucceeds() {
    PipelineExecution execution = runningExecution();
    when(executionRepository.findByStatus(ExecutionStatus.RUNNING)).thenReturn(List.of(execution));

    Job complete = job(2, true, false, null);
    JobList list = new JobList();
    list.setItems(List.of(complete));
    when(labeledOp.list()).thenReturn(list);
    when(stepRepository.findByPipelineIdOrdered("p1"))
        .thenReturn(List.of(step(1, "plet-a"), step(2, "plet-b")));

    poller.poll();

    verify(orchestrator).markCompleted("exec-1", 1L, 1L);
  }

  @Test
  void marksFailedOnImagePullBackOffBeforeJobFailed() {
    PipelineExecution execution = runningExecution();
    when(executionRepository.findByStatus(ExecutionStatus.RUNNING)).thenReturn(List.of(execution));

    Job pending = job(1, false, false, null);
    pending.getMetadata().getLabels().put(PipeletK8sLabels.PIPELET_ID, "plet-rest-source");
    JobList list = new JobList();
    list.setItems(List.of(pending));
    when(labeledOp.list()).thenReturn(list);

    Pod pod = new Pod();
    ObjectMeta podMeta = new ObjectMeta();
    podMeta.setName("exec-exec-1-stage-1-xyz");
    pod.setMetadata(podMeta);
    PodStatus podStatus = new PodStatus();
    podStatus.setPhase("Pending");
    ContainerStatus cs = new ContainerStatus();
    cs.setName("pipelet");
    cs.setImage("dashflow/plet-rest-source:local");
    ContainerStateWaiting waiting = new ContainerStateWaiting();
    waiting.setReason("ImagePullBackOff");
    waiting.setMessage("pull access denied for dashflow/plet-rest-source");
    ContainerState state = new ContainerState();
    state.setWaiting(waiting);
    cs.setState(state);
    podStatus.setContainerStatuses(List.of(cs));
    pod.setStatus(podStatus);

    PodList podList = new PodList();
    podList.setItems(List.of(pod));

    when(kubernetesClient.pods()).thenReturn(podsOp);
    doReturn(podsNsOp).when(podsOp).inNamespace(anyString());
    doReturn(podsLabeledOp).when(podsNsOp).withLabel(anyString(), anyString());
    // second withLabel for stage_order chains off first FilterWatchListDeletable
    doReturn(podsLabeledOp).when(podsLabeledOp).withLabel(anyString(), anyString());
    when(podsLabeledOp.list()).thenReturn(podList);

    poller.poll();

    verify(orchestrator).markFailed(eq("exec-1"), contains("Image pull failed"));
    verify(orchestrator).markFailed(eq("exec-1"), contains("build-pipelets"));
    verify(pipeletJobClient, never()).create(any());
  }

  private static PipelineExecution runningExecution() {
    PipelineExecution execution = new PipelineExecution();
    execution.setId("exec-1");
    execution.setPipelineId("p1");
    execution.setTenantId("T001");
    execution.setStatus(ExecutionStatus.RUNNING);
    return execution;
  }

  private static PipelineStep step(int order, String pipeletId) {
    PipelineStep step = new PipelineStep();
    step.setId("s" + order);
    step.setPipelineId("p1");
    step.setStepOrder(order);
    step.setPipeletId(pipeletId);
    return step;
  }

  private static Job job(int stage, boolean complete, boolean failed, String reason) {
    Job job = new Job();
    ObjectMeta meta = new ObjectMeta();
    meta.setName("exec-exec-1-stage-" + stage);
    meta.setLabels(
        new HashMap<>(Map.of(PipeletK8sLabels.STAGE_ORDER, String.valueOf(stage))));
    job.setMetadata(meta);
    JobStatus status = new JobStatus();
    if (complete) {
      status.setSucceeded(1);
      JobCondition c = new JobCondition();
      c.setType("Complete");
      c.setStatus("True");
      status.setConditions(List.of(c));
    }
    if (failed) {
      status.setFailed(3);
      JobCondition c = new JobCondition();
      c.setType("Failed");
      c.setStatus("True");
      c.setReason(reason);
      c.setMessage(reason);
      status.setConditions(List.of(c));
    }
    job.setStatus(status);
    return job;
  }
}
