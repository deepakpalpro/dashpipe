package com.dashflow.api.k8s;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds architecture §10.3 pipelet Job manifests from {@link PipeletJobRequest}. */
public final class PipeletJobManifestFactory {

  private PipeletJobManifestFactory() {}

  public static Job build(PipeletJobRequest request, PipeletK8sProperties props) {
    return build(request, props, Map.of());
  }

  public static Job build(
      PipeletJobRequest request, PipeletK8sProperties props, Map<String, String> brokerEnv) {
    if (request == null) {
      throw new IllegalArgumentException("PipeletJobRequest is required");
    }
    PipeletK8sProperties config = props == null ? new PipeletK8sProperties() : props;

    String namespace = PipeletK8sProperties.namespaceForTenant(request.tenantId());
    String jobName = PipeletK8sProperties.sanitizeJobName(request.jobName());
    String image = config.resolveImage(request.pipeletId());

    Map<String, String> labels = new LinkedHashMap<>();
    labels.put("app.kubernetes.io/name", "pipelet");
    labels.put("app.kubernetes.io/managed-by", "dashflow");
    labels.put(PipeletK8sLabels.TENANT_ID, nullToEmpty(request.tenantId()));
    labels.put(PipeletK8sLabels.PIPELINE_ID, nullToEmpty(request.pipelineId()));
    labels.put(PipeletK8sLabels.EXECUTION_ID, nullToEmpty(request.executionId()));
    labels.put(PipeletK8sLabels.PIPELET_ID, nullToEmpty(request.pipeletId()));
    labels.put(PipeletK8sLabels.STAGE_ORDER, String.valueOf(request.stageOrder()));

    List<EnvVar> env = new ArrayList<>();
    env.add(env("TENANT_ID", request.tenantId()));
    env.add(env("PIPELINE_ID", request.pipelineId()));
    env.add(env("EXECUTION_ID", request.executionId()));
    env.add(env("PIPELET_ID", request.pipeletId()));
    env.add(env("STAGE_ORDER", String.valueOf(request.stageOrder())));
    env.add(env("STAGE_COUNT", String.valueOf(request.stageCount())));
    env.add(env("IO_MODE", request.ioMode() == null ? "queue" : request.ioMode()));
    if (request.inputQueue() != null && !request.inputQueue().isBlank()) {
      env.add(env("INPUT_QUEUE", request.inputQueue()));
    }
    if (request.outputQueue() != null && !request.outputQueue().isBlank()) {
      env.add(env("OUTPUT_QUEUE", request.outputQueue()));
    }
    if (brokerEnv != null) {
      brokerEnv.forEach((k, v) -> {
        if (k != null && !k.isBlank() && v != null && !v.isBlank()) {
          env.add(env(k, v));
        }
      });
    }
    if (request.amqpUrl() != null && !request.amqpUrl().isBlank()) {
      // Request override wins over broker defaults for AMQP_URL.
      env.add(env("AMQP_URL", request.amqpUrl()));
    }
    // Sources in queue mode can process once without waiting for a kickoff message.
    env.add(env("SOURCE_TRIGGER", "once"));
    // Pipelet config layers (merged further inside the container).
    if (request.connectorConfig() != null && !request.connectorConfig().isBlank()) {
      env.add(env("CONNECTOR_CONFIG", request.connectorConfig()));
    }
    if (request.serviceConfig() != null && !request.serviceConfig().isBlank()) {
      env.add(env("SERVICE_CONFIG", request.serviceConfig()));
    }
    if (request.deploymentConfig() != null && !request.deploymentConfig().isBlank()) {
      env.add(env("DEPLOYMENT_CONFIG", request.deploymentConfig()));
    }
    if (request.executionConfig() != null && !request.executionConfig().isBlank()) {
      env.add(env("EXECUTION_CONFIG", request.executionConfig()));
    }
    if (request.triggerPayload() != null && !request.triggerPayload().isBlank()) {
      env.add(env("TRIGGER_PAYLOAD", request.triggerPayload()));
    }

    return new JobBuilder()
        .withNewMetadata()
        .withName(jobName)
        .withNamespace(namespace)
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withBackoffLimit(config.getBackoffLimit())
        .withTtlSecondsAfterFinished(config.getTtlSecondsAfterFinished())
        .withNewTemplate()
        .withNewMetadata()
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withRestartPolicy("Never")
        .addToContainers(
            new ContainerBuilder()
                .withName("pipelet")
                .withImage(image)
                .withImagePullPolicy(config.getImagePullPolicy())
                .withEnv(env)
                .withResources(
                    new ResourceRequirementsBuilder()
                        .addToRequests("cpu", new Quantity(config.getCpuRequest()))
                        .addToRequests("memory", new Quantity(config.getMemoryRequest()))
                        .addToLimits("cpu", new Quantity(config.getCpuLimit()))
                        .addToLimits("memory", new Quantity(config.getMemoryLimit()))
                        .build())
                .build())
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private static EnvVar env(String name, String value) {
    return new EnvVarBuilder().withName(name).withValue(value == null ? "" : value).build();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
