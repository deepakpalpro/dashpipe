package com.dashpipe.api.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.api.connector.TenantConnector;
import com.dashpipe.api.connector.TenantConnectorRepository;
import com.dashpipe.api.pipeline.Pipeline;
import com.dashpipe.api.pipeline.PipelineStep;
import com.dashpipe.api.service.SecretEncryptor;
import com.dashpipe.api.service.TenantServiceConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PipeletStepEnvResolverTest {

  @Mock private TenantConnectorRepository connectorRepository;
  @Mock private TenantServiceConfigRepository serviceRepository;

  private PipeletStepEnvResolver resolver;

  @BeforeEach
  void setUp() {
    ObjectMapper mapper = new ObjectMapper();
    resolver =
        new PipeletStepEnvResolver(
            mapper, connectorRepository, serviceRepository, new SecretEncryptor(mapper));
  }

  @Test
  void resolve_mergesConnectorAndStepConfigs() {
    Pipeline pipeline = new Pipeline();
    pipeline.setId("p1");
    pipeline.setTenantId("T001");
    pipeline.setDeploymentConfig("{\"cloud\":\"aws\"}");

    PipelineStep step = new PipelineStep();
    step.setConnectorIds("[\"c1\"]");
    step.setServiceIds("[]");
    step.setDeploymentConfig("{\"region\":\"us-east-1\"}");
    step.setExecutionConfig("{\"objectKey\":\"inventory/daily.csv\"}");

    TenantConnector connector = new TenantConnector();
    connector.setId("c1");
    connector.setTenantId("T001");
    connector.setExecutionConfig(
        "{\"bucket\":\"demo-s3-source\",\"endpoint\":\"http://host.docker.internal:4567\",\"password\":\"encrypted:test\"}");
    when(connectorRepository.findByIdAndTenantId(eq("c1"), eq("T001")))
        .thenReturn(Optional.of(connector));

    PipeletJobEnv env = resolver.resolve(pipeline, step);

    assertThat(env.connectorConfig())
        .contains("\"bucket\":\"demo-s3-source\"")
        .contains("\"password\":\"test\"")
        .doesNotContain("encrypted:");
    assertThat(env.deploymentConfig()).contains("\"cloud\":\"aws\"").contains("\"region\":\"us-east-1\"");
    assertThat(env.executionConfig()).contains("\"objectKey\":\"inventory/daily.csv\"");
    assertThat(env.serviceConfig()).isEqualTo("{}");
  }
}
