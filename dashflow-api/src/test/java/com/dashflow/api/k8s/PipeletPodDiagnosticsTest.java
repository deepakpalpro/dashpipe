package com.dashflow.api.k8s;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipeletPodDiagnosticsTest {

  @Test
  void detectsImagePullBackOff() {
    Pod pod = waitingPod("ImagePullBackOff", "pull access denied for dashflow/plet-rest-source");
    assertThat(PipeletPodDiagnostics.diagnose(List.of(pod)))
        .isPresent()
        .get()
        .asString()
        .contains("Image pull failed")
        .contains("dashflow/plet-rest-source:local")
        .contains("build-pipelets");
  }

  @Test
  void detectsErrImagePull() {
    Pod pod = waitingPod("ErrImagePull", "repository does not exist");
    assertThat(PipeletPodDiagnostics.isImagePullFailure("ErrImagePull")).isTrue();
    assertThat(PipeletPodDiagnostics.diagnose(List.of(pod))).isPresent();
  }

  @Test
  void ignoresHealthyRunning() {
    Pod pod = new Pod();
    ObjectMeta meta = new ObjectMeta();
    meta.setName("ok");
    pod.setMetadata(meta);
    PodStatus status = new PodStatus();
    status.setPhase("Running");
    ContainerStatus cs = new ContainerStatus();
    cs.setName("pipelet");
    cs.setImage("dashflow/plet-rest-source:local");
    ContainerState state = new ContainerState();
    cs.setState(state);
    status.setContainerStatuses(List.of(cs));
    pod.setStatus(status);
    assertThat(PipeletPodDiagnostics.diagnose(List.of(pod))).isEmpty();
  }

  private static Pod waitingPod(String reason, String message) {
    Pod pod = new Pod();
    ObjectMeta meta = new ObjectMeta();
    meta.setName("exec-1-stage-1-abc");
    pod.setMetadata(meta);
    PodStatus status = new PodStatus();
    status.setPhase("Pending");
    ContainerStatus cs = new ContainerStatus();
    cs.setName("pipelet");
    cs.setImage("dashflow/plet-rest-source:local");
    ContainerStateWaiting waiting = new ContainerStateWaiting();
    waiting.setReason(reason);
    waiting.setMessage(message);
    ContainerState state = new ContainerState();
    state.setWaiting(waiting);
    cs.setState(state);
    status.setContainerStatuses(List.of(cs));
    pod.setStatus(status);
    return pod;
  }
}
