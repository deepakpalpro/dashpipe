package com.dashpipe.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dashpipe.api.connector.CreateConnectorRequest;
import com.dashpipe.api.connector.TenantConnectorResponse;
import com.dashpipe.api.connector.TenantConnectorService;
import com.dashpipe.api.service.CreateTenantServiceRequest;
import com.dashpipe.api.service.TenantServiceConfigService;
import com.dashpipe.api.service.TenantServiceResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PipelineBundleServiceTest {

  @Mock private PipelineService pipelineService;
  @Mock private PipelineStepsService pipelineStepsService;
  @Mock private TenantConnectorService tenantConnectorService;
  @Mock private TenantServiceConfigService tenantServiceConfigService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private PipelineBundleService bundleService;

  @BeforeEach
  void setUp() {
    bundleService =
        new PipelineBundleService(
            pipelineService,
            pipelineStepsService,
            tenantConnectorService,
            tenantServiceConfigService,
            objectMapper);
  }

  @Test
  void export_includesReferencedConnectorsAndServices() {
    ObjectNode dep = objectMapper.createObjectNode().put("region", "us-east-1");
    ObjectNode exec = objectMapper.createObjectNode().put("delimiter", ",");
    when(pipelineService.get("p1"))
        .thenReturn(
            new PipelineResponse(
                "p1",
                "T001",
                "Inventory",
                null,
                PipelineVisibility.PRIVATE,
                PipelineExecutionMode.ASYNC,
                1,
                PipelineStatus.ACTIVE,
                dep,
                objectMapper.createObjectNode().put("ioMode", "queue"),
                Instant.now(),
                Instant.now(),
                List.of(
                    new PipelineStepResponse(
                        "s1",
                        "plet-csv-to-json",
                        1,
                        exec,
                        dep,
                        exec,
                        List.of("c1"),
                        List.of("svc1"),
                        "q.in",
                        "q.out",
                        null))));
    when(tenantConnectorService.get("c1"))
        .thenReturn(
            new TenantConnectorResponse(
                "c1",
                "T001",
                "ct-rest",
                "Petstore",
                objectMapper.createObjectNode().put("baseUrl", "http://x"),
                DualEmpty(),
                objectMapper.createObjectNode().put("baseUrl", "http://x"),
                null,
                null,
                Instant.now()));
    when(tenantServiceConfigService.get("svc1"))
        .thenReturn(
            new TenantServiceResponse(
                "svc1",
                "T001",
                "st-auth",
                "okta",
                "Auth",
                objectMapper.createObjectNode().put("client_id", "abc"),
                DualEmpty(),
                objectMapper.createObjectNode().put("client_id", "abc"),
                true,
                null,
                Instant.now()));

    PipelineBundle bundle = bundleService.export("p1");

    assertThat(bundle.formatVersion()).isEqualTo("1");
    assertThat(bundle.pipeline().name()).isEqualTo("Inventory");
    assertThat(bundle.pipeline().executionConfig().get("ioMode").asText()).isEqualTo("queue");
    assertThat(bundle.steps()).hasSize(1);
    assertThat(bundle.steps().get(0).connectorRefs()).containsExactly("ct-rest::Petstore");
    assertThat(bundle.steps().get(0).serviceRefs()).containsExactly("st-auth::okta::Auth");
    assertThat(bundle.connectors()).hasSize(1);
    assertThat(bundle.services()).hasSize(1);
  }

  @Test
  void import_createsPipelineStepsAndDeps() {
    when(pipelineService.list()).thenReturn(List.of());
    when(tenantConnectorService.list()).thenReturn(List.of());
    when(tenantServiceConfigService.list()).thenReturn(List.of());
    when(tenantConnectorService.create(any(CreateConnectorRequest.class)))
        .thenAnswer(
            inv -> {
              CreateConnectorRequest req = inv.getArgument(0);
              return new TenantConnectorResponse(
                  "new-c",
                  "T001",
                  req.connectorTypeId(),
                  req.name(),
                  req.executionConfig(),
                  req.deploymentConfig(),
                  req.executionConfig(),
                  null,
                  null,
                  Instant.now());
            });
    when(tenantServiceConfigService.create(any(CreateTenantServiceRequest.class)))
        .thenAnswer(
            inv -> {
              CreateTenantServiceRequest req = inv.getArgument(0);
              return new TenantServiceResponse(
                  "new-s",
                  "T001",
                  req.serviceTypeId(),
                  req.vendor(),
                  req.name(),
                  req.executionConfig(),
                  req.deploymentConfig(),
                  req.executionConfig(),
                  true,
                  null,
                  Instant.now());
            });
    when(pipelineService.create(any(CreatePipelineRequest.class)))
        .thenReturn(
            new PipelineResponse(
                "new-p",
                "T001",
                "Imported",
                null,
                PipelineVisibility.PRIVATE,
                PipelineExecutionMode.ASYNC,
                1,
                PipelineStatus.DRAFT,
                DualEmpty(),
                DualEmpty(),
                Instant.now(),
                Instant.now(),
                List.of()));
    when(pipelineStepsService.replace(eq("new-p"), any(ReplacePipelineStepsRequest.class)))
        .thenReturn(
            new PipelineResponse(
                "new-p",
                "T001",
                "Imported",
                null,
                PipelineVisibility.PRIVATE,
                PipelineExecutionMode.ASYNC,
                2,
                PipelineStatus.ACTIVE,
                DualEmpty(),
                DualEmpty(),
                Instant.now(),
                Instant.now(),
                List.of()));

    PipelineBundle bundle =
        new PipelineBundle(
            "1",
            Instant.now(),
            new PipelineBundlePipeline(
                "Imported",
                null,
                PipelineVisibility.PRIVATE,
                PipelineExecutionMode.ASYNC,
                DualEmpty(),
                objectMapper.createObjectNode().put("ioMode", "stdio")),
            List.of(
                new PipelineBundleStep(
                    "plet-csv-to-json",
                    1,
                    DualEmpty(),
                    objectMapper.createObjectNode().put("delimiter", ","),
                    List.of("ct-rest::Petstore"),
                    List.of("st-auth::okta::Auth"),
                    null,
                    null,
                    null)),
            List.of(
                new PipelineBundleConnector(
                    "ct-rest::Petstore",
                    "ct-rest",
                    "Petstore",
                    DualEmpty(),
                    objectMapper.createObjectNode().put("baseUrl", "http://x"))),
            List.of(
                new PipelineBundleServiceEntry(
                    "st-auth::okta::Auth",
                    "st-auth",
                    "okta",
                    "Auth",
                    true,
                    DualEmpty(),
                    objectMapper.createObjectNode().put("client_id", "abc"))));

    PipelineBundleImportRequest.Result result =
        bundleService.importBundle(new PipelineBundleImportRequest(bundle, null, "create"));

    assertThat(result.pipelineId()).isEqualTo("new-p");
    assertThat(result.createdConnectors()).contains("ct-rest::Petstore");
    assertThat(result.createdServices()).contains("st-auth::okta::Auth");

    ArgumentCaptor<ReplacePipelineStepsRequest> stepsCaptor =
        ArgumentCaptor.forClass(ReplacePipelineStepsRequest.class);
    verify(pipelineStepsService).replace(eq("new-p"), stepsCaptor.capture());
    assertThat(stepsCaptor.getValue().steps().get(0).connectorIds()).containsExactly("new-c");
    assertThat(stepsCaptor.getValue().steps().get(0).serviceIds()).containsExactly("new-s");
  }

  private ObjectNode DualEmpty() {
    return objectMapper.createObjectNode();
  }
}
