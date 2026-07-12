package com.dashflow.api.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dashflow.api.pipeline.PipeletAmqpUrlFactory;
import com.dashflow.api.pipeline.PipelineOrchestrationProperties;
import com.dashflow.spi.messaging.MessageBroker;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Live smoke against the current kubeconfig (Rancher Desktop). Skips when the API is unreachable.
 */
class Fabric8PipeletJobClientIT {

  private KubernetesClient client;
  private Fabric8PipeletJobClient jobClient;
  private String createdJobName;
  private String createdNamespace;

  @BeforeEach
  void setUp() {
    assumeTrue(clusterReachable(), "Kubernetes API not reachable (start Rancher Desktop)");
    client = new KubernetesClientBuilder().build();
    PipeletK8sProperties props = new PipeletK8sProperties();
    props.setEnabled(true);
    props.setCreateNamespace(true);
    props.setImages(Map.of("plet-csv-to-json", "busybox:1.36"));
    props.setDefaultImagePattern("busybox:1.36");
    MessageBroker broker = mock(MessageBroker.class);
    when(broker.pipeletConnectionEnv())
        .thenReturn(
            Map.of(
                "IO_BROKER",
                "rabbitmq",
                "AMQP_URL",
                "amqp://pipeline:pipeline@host.docker.internal:5672/"));
    PipeletAmqpUrlFactory amqpFactory =
        new PipeletAmqpUrlFactory(broker, new PipelineOrchestrationProperties());
    jobClient = new Fabric8PipeletJobClient(client, props, amqpFactory);
  }

  @AfterEach
  void tearDown() {
    if (client != null && createdJobName != null && createdNamespace != null) {
      try {
        client
            .batch()
            .v1()
            .jobs()
            .inNamespace(createdNamespace)
            .withName(createdJobName)
            .withPropagationPolicy(
                io.fabric8.kubernetes.api.model.DeletionPropagation.BACKGROUND)
            .delete();
      } catch (Exception ignored) {
        // best-effort cleanup
      }
    }
    if (client != null) {
      client.close();
    }
  }

  @Test
  void create_spawnsJobInTenantNamespace() {
    String executionId = "it-" + UUID.randomUUID().toString().substring(0, 8);
    PipeletJobRequest request =
        PipeletJobRequest.of(
            "T001",
            "pipe-fabric8-it",
            executionId,
            "plet-csv-to-json",
            1,
            1,
            "smoke.in",
            null,
            "stdio",
            null);

    PipeletJobHandle handle = jobClient.create(request);

    createdNamespace = handle.namespace();
    createdJobName = handle.jobName();

    assertThat(handle.namespace()).isEqualTo("tenant-t001");
    assertThat(handle.status()).isEqualTo("created");

    Job job =
        client.batch().v1().jobs().inNamespace(createdNamespace).withName(createdJobName).get();
    assertThat(job).isNotNull();
    assertThat(job.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
        .isEqualTo("busybox:1.36");
    assertThat(job.getMetadata().getLabels())
        .containsEntry("pipeline.platform/pipelet_id", "plet-csv-to-json");
  }

  private static boolean clusterReachable() {
    try (KubernetesClient probe = new KubernetesClientBuilder().build()) {
      // Prefer a lightweight list over getKubernetesVersion(): newer API servers
      // return fields (e.g. emulationMajor) that older Fabric8 VersionInfo rejects.
      probe.namespaces().list();
      return true;
    } catch (Exception ex) {
      return false;
    }
  }
}
