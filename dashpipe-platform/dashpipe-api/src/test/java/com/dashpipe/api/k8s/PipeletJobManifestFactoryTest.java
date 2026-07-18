package com.dashpipe.api.k8s;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PipeletJobManifestFactoryTest {

  @Test
  void buildsJobWithIoEnvAndDnsSafeNames() {
    PipeletK8sProperties props = new PipeletK8sProperties();
    props.setImages(Map.of("plet-csv-to-json", "dashpipe/plet-csv-to-json:local"));

    PipeletJobRequest request =
        PipeletJobRequest.of(
                "T001",
                "pipe-abc",
                "exec-1",
                "plet-csv-to-json",
                1,
                2,
                "tenant.T001.pipeline.pipe-abc.stage.1.in",
                "tenant.T001.pipeline.pipe-abc.stage.2.in",
                "queue",
                "amqp://pipeline:pipeline@host.docker.internal:5672/")
            .withEnv(
                new PipeletJobEnv(
                    "{\"bucket\":\"demo\"}",
                    "{}",
                    "{\"region\":\"us-east-1\"}",
                    "{\"objectKey\":\"inventory/daily.csv\"}"));

    Job job = PipeletJobManifestFactory.build(request, props);

    assertThat(job.getMetadata().getNamespace()).isEqualTo("tenant-t001");
    assertThat(job.getMetadata().getName()).isEqualTo("exec-exec-1-stage-1");
    assertThat(job.getMetadata().getLabels())
        .containsEntry(PipeletK8sLabels.PIPELET_ID, "plet-csv-to-json");

    Map<String, String> env =
        job.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().stream()
            .collect(Collectors.toMap(EnvVar::getName, EnvVar::getValue));

    assertThat(env)
        .containsEntry("IO_MODE", "queue")
        .containsEntry("INPUT_QUEUE", "tenant.T001.pipeline.pipe-abc.stage.1.in")
        .containsEntry("OUTPUT_QUEUE", "tenant.T001.pipeline.pipe-abc.stage.2.in")
        .containsEntry("AMQP_URL", "amqp://pipeline:pipeline@host.docker.internal:5672/")
        .containsEntry("PIPELET_ID", "plet-csv-to-json")
        .containsEntry("SOURCE_TRIGGER", "once")
        .containsEntry("CONNECTOR_CONFIG", "{\"bucket\":\"demo\"}")
        .containsEntry("SERVICE_CONFIG", "{}")
        .containsEntry("DEPLOYMENT_CONFIG", "{\"region\":\"us-east-1\"}")
        .containsEntry("EXECUTION_CONFIG", "{\"objectKey\":\"inventory/daily.csv\"}");

    assertThat(job.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
        .isEqualTo("dashpipe/plet-csv-to-json:local");
    assertThat(job.getSpec().getTemplate().getSpec().getRestartPolicy()).isEqualTo("Never");
  }

  @Test
  void injectsTriggerPayloadEnvWhenPresent() {
    PipeletJobRequest request =
        PipeletJobRequest.of(
                "T001", "pipe-abc", "exec-1", "plet-manual-source", 1, 1, null, null)
            .withTriggerPayload("{\"hello\":\"world\"}");

    Job job = PipeletJobManifestFactory.build(request, new PipeletK8sProperties());
    Map<String, String> env =
        job.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().stream()
            .collect(Collectors.toMap(EnvVar::getName, EnvVar::getValue));

    assertThat(env).containsEntry("TRIGGER_PAYLOAD", "{\"hello\":\"world\"}");
  }

  @Test
  void resolvesDefaultImagePattern() {
    PipeletK8sProperties props = new PipeletK8sProperties();
    assertThat(props.resolveImage("plet-python-filter"))
        .isEqualTo("dashpipe/plet-python-filter:local");
  }
}
