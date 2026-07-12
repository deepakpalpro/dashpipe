package com.dashflow.api.k8s;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Detects terminal / stuck pod failures that may not yet set Job {@code Failed} (e.g.
 * ImagePullBackOff), so executions do not hang indefinitely.
 */
final class PipeletPodDiagnostics {

  private static final Set<String> IMAGE_PULL_REASONS =
      Set.of(
          "imagepullbackoff",
          "errimagepull",
          "invalidimagename",
          "imageinspecterror");

  private static final Set<String> STUCK_WAIT_REASONS =
      Set.of(
          "createcontainerconfigerror",
          "createcontainererror",
          "crashloopbackoff",
          "runcontainererror");

  private PipeletPodDiagnostics() {}

  static Optional<String> diagnose(List<Pod> pods) {
    if (pods == null || pods.isEmpty()) {
      return Optional.empty();
    }
    for (Pod pod : pods) {
      Optional<String> fromContainers = diagnoseContainerStatuses(pod);
      if (fromContainers.isPresent()) {
        return fromContainers;
      }
      Optional<String> fromPod = diagnosePodPhase(pod);
      if (fromPod.isPresent()) {
        return fromPod;
      }
    }
    return Optional.empty();
  }

  static boolean isImagePullFailure(String reason) {
    if (reason == null || reason.isBlank()) {
      return false;
    }
    return IMAGE_PULL_REASONS.contains(reason.trim().toLowerCase(Locale.ROOT));
  }

  private static Optional<String> diagnoseContainerStatuses(Pod pod) {
    PodStatus status = pod.getStatus();
    if (status == null || status.getContainerStatuses() == null) {
      return Optional.empty();
    }
    String podName =
        pod.getMetadata() != null && pod.getMetadata().getName() != null
            ? pod.getMetadata().getName()
            : "unknown-pod";
    for (ContainerStatus cs : status.getContainerStatuses()) {
      if (cs == null) {
        continue;
      }
      ContainerState state = cs.getState();
      if (state == null) {
        continue;
      }
      ContainerStateWaiting waiting = state.getWaiting();
      if (waiting == null) {
        continue;
      }
      String reason = waiting.getReason();
      String message = waiting.getMessage() == null ? "" : waiting.getMessage().trim();
      String image = cs.getImage() == null ? "" : cs.getImage();
      String container = cs.getName() == null ? "pipelet" : cs.getName();

      if (isImagePullFailure(reason)) {
        return Optional.of(
            "Image pull failed for container '"
                + container
                + "' image='"
                + image
                + "' (pod "
                + podName
                + "): "
                + reason
                + (message.isBlank() ? "" : " — " + message)
                + ". Build/load the image into the cluster Docker store"
                + " (./scripts/localdev.sh build-pipelets) and re-run."
                + " Tip: k8s_POD_* is the pause container — use logs -c pipelet.");
      }
      if (reason != null
          && STUCK_WAIT_REASONS.contains(reason.trim().toLowerCase(Locale.ROOT))) {
        return Optional.of(
            "Pod container stuck waiting: container='"
                + container
                + "' image='"
                + image
                + "' (pod "
                + podName
                + "): "
                + reason
                + (message.isBlank() ? "" : " — " + message));
      }
    }
    return Optional.empty();
  }

  private static Optional<String> diagnosePodPhase(Pod pod) {
    PodStatus status = pod.getStatus();
    if (status == null) {
      return Optional.empty();
    }
    String phase = status.getPhase();
    String reason = status.getReason();
    if ("Failed".equalsIgnoreCase(phase)
        || (reason != null && isImagePullFailure(reason))) {
      String podName =
          pod.getMetadata() != null && pod.getMetadata().getName() != null
              ? pod.getMetadata().getName()
              : "unknown-pod";
      String message = status.getMessage() == null ? "" : status.getMessage().trim();
      return Optional.of(
          "Pod "
              + podName
              + " phase="
              + phase
              + (reason == null || reason.isBlank() ? "" : " reason=" + reason)
              + (message.isBlank() ? "" : " — " + message));
    }
    return Optional.empty();
  }
}
