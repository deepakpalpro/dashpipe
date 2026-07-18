package com.dashpipe.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dashpipe.api.tenant.TenantContext;
import com.dashpipe.api.tenant.TenantFilters;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
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
class PipelineStepsServiceTest {

  @Mock private PipelineRepository pipelineRepository;
  @Mock private PipelineStepRepository pipelineStepRepository;
  @Mock private EntityManager entityManager;
  @Mock private Session session;
  @Mock private Filter hibernateFilter;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private PipelineStepsService pipelineStepsService;

  @BeforeEach
  void setUp() {
    pipelineStepsService =
        new PipelineStepsService(
            pipelineRepository, pipelineStepRepository, entityManager, objectMapper);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.getEnabledFilter(TenantFilters.NAME)).thenReturn(null);
    when(session.enableFilter(TenantFilters.NAME)).thenReturn(hibernateFilter);
  }

  @AfterEach
  void clearContext() {
    TenantContext.clear();
  }

  @Test
  void replace_ordersSteps() {
    TenantContext.setTenantId("tenant-a");
    Pipeline pipeline = existingPipeline();
    when(pipelineRepository.findFilteredById("p1")).thenReturn(Optional.of(pipeline));
    when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(inv -> inv.getArgument(0));
    when(pipelineStepRepository.save(any(PipelineStep.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ObjectNode config = objectMapper.createObjectNode().put("batch_size", 100);
    ReplacePipelineStepsRequest request =
        new ReplacePipelineStepsRequest(
            List.of(
                new PipelineStepRequest(
                    "plet-b",
                    2,
                    null, null, null,
                    List.of(),
                    List.of(),
                    "q.in.2",
                    "q.out.2",
                    null),
                new PipelineStepRequest(
                    "plet-a",
                    1,
                    config, null, null,
                    List.of("conn-1"),
                    List.of("svc-1"),
                    "q.in.1",
                    "q.out.1",
                    null)));

    PipelineResponse response = pipelineStepsService.replace("p1", request);

    verify(pipelineStepRepository).deleteByPipelineId("p1");
    ArgumentCaptor<PipelineStep> stepCaptor = ArgumentCaptor.forClass(PipelineStep.class);
    verify(pipelineStepRepository, org.mockito.Mockito.times(2)).save(stepCaptor.capture());
    List<PipelineStep> saved = stepCaptor.getAllValues();
    assertThat(saved).extracting(PipelineStep::getStepOrder).containsExactly(1, 2);
    assertThat(saved).extracting(PipelineStep::getPipeletId).containsExactly("plet-a", "plet-b");
    assertThat(response.steps()).hasSize(2);
    assertThat(response.steps().get(0).stepOrder()).isEqualTo(1);
    assertThat(response.steps().get(1).stepOrder()).isEqualTo(2);
    assertThat(response.version()).isEqualTo(2);
    assertThat(response.status()).isEqualTo(PipelineStatus.ACTIVE);
    verify(hibernateFilter).setParameter(anyString(), any());
  }

  @Test
  void replace_rejectsDuplicateStepOrder() {
    TenantContext.setTenantId("tenant-a");
    when(pipelineRepository.findFilteredById("p1")).thenReturn(Optional.of(existingPipeline()));

    ReplacePipelineStepsRequest request =
        new ReplacePipelineStepsRequest(
            List.of(
                new PipelineStepRequest("a", 1, null, null, null, null, null, null, null, null),
                new PipelineStepRequest("b", 1, null, null, null, null, null, null, null, null)));

    assertThatThrownBy(() -> pipelineStepsService.replace("p1", request))
        .isInstanceOf(PipelineValidationException.class)
        .hasMessageContaining("Duplicate step_order");

    verify(pipelineStepRepository, never()).deleteByPipelineId(anyString());
    verify(pipelineStepRepository, never()).save(any());
  }

  @Test
  void replace_missingPipeline_throwsNotFound() {
    TenantContext.setTenantId("tenant-a");
    when(pipelineRepository.findFilteredById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                pipelineStepsService.replace(
                    "missing",
                    new ReplacePipelineStepsRequest(
                        List.of(
                            new PipelineStepRequest(
                                "plet", 1, null, null, null, null, null, null, null, null)))))
        .isInstanceOf(PipelineNotFoundException.class);
  }

  private static Pipeline existingPipeline() {
    Pipeline pipeline = new Pipeline();
    pipeline.setId("p1");
    pipeline.setTenantId("tenant-a");
    pipeline.setName("pipe");
    pipeline.setVisibility(PipelineVisibility.PRIVATE);
    pipeline.setExecutionMode(PipelineExecutionMode.ASYNC);
    pipeline.setStatus(PipelineStatus.DRAFT);
    pipeline.setVersion(1);
    return pipeline;
  }
}
