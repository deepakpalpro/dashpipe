package com.dashpipe.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.api.tenant.TenantContext;
import com.dashpipe.api.tenant.TenantContextRequiredException;
import com.dashpipe.api.tenant.TenantFilters;
import jakarta.persistence.EntityManager;
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
class PipelineServiceTest {

  @Mock private PipelineRepository pipelineRepository;
  @Mock private PipelineStepsService pipelineStepsService;
  @Mock private EntityManager entityManager;
  @Mock private Session session;
  @Mock private Filter hibernateFilter;

  private PipelineService pipelineService;

  @BeforeEach
  void setUp() {
    pipelineService =
        new PipelineService(
            pipelineRepository, pipelineStepsService, entityManager, new ObjectMapper());
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.getEnabledFilter(TenantFilters.NAME)).thenReturn(null);
    when(session.enableFilter(TenantFilters.NAME)).thenReturn(hibernateFilter);
    when(pipelineStepsService.loadSteps(anyString())).thenReturn(java.util.List.of());
  }

  @AfterEach
  void clearContext() {
    TenantContext.clear();
  }

  @Test
  void create_defaultsDraftAsyncPrivate() {
    TenantContext.setTenantId("tenant-a");
    when(pipelineRepository.existsByTenantIdAndName("tenant-a", "customer-sync")).thenReturn(false);
    when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(inv -> inv.getArgument(0));

    PipelineResponse response =
        pipelineService.create(new CreatePipelineRequest("customer-sync", null, null, null, null, null));

    ArgumentCaptor<Pipeline> captor = ArgumentCaptor.forClass(Pipeline.class);
    verify(pipelineRepository).save(captor.capture());
    Pipeline saved = captor.getValue();

    assertThat(saved.getVisibility()).isEqualTo(PipelineVisibility.PRIVATE);
    assertThat(saved.getExecutionMode()).isEqualTo(PipelineExecutionMode.ASYNC);
    assertThat(saved.getStatus()).isEqualTo(PipelineStatus.DRAFT);
    assertThat(saved.getVersion()).isEqualTo(1);
    assertThat(response.visibility()).isEqualTo(PipelineVisibility.PRIVATE);
    assertThat(response.executionMode()).isEqualTo(PipelineExecutionMode.ASYNC);
    assertThat(response.status()).isEqualTo(PipelineStatus.DRAFT);
    assertThat(response.steps()).isEmpty();
  }

  @Test
  void create_requiresTenantContext() {
    assertThatThrownBy(
            () ->
                pipelineService.create(
                    new CreatePipelineRequest("x", null, PipelineVisibility.PRIVATE, null, null, null)))
        .isInstanceOf(TenantContextRequiredException.class);

    verify(pipelineRepository, never()).save(any());
  }

  @Test
  void create_rejectsDuplicateName() {
    TenantContext.setTenantId("tenant-a");
    when(pipelineRepository.existsByTenantIdAndName("tenant-a", "dup")).thenReturn(true);

    assertThatThrownBy(
            () ->
                pipelineService.create(
                    new CreatePipelineRequest("dup", null, null, PipelineExecutionMode.ASYNC, null, null)))
        .isInstanceOf(PipelineConflictException.class);

    verify(pipelineRepository, never()).save(any());
  }

  @Test
  void archive_setsArchivedStatus() {
    TenantContext.setTenantId("tenant-a");
    Pipeline existing = new Pipeline();
    existing.setId("p1");
    existing.setTenantId("tenant-a");
    existing.setName("pipe");
    existing.setVisibility(PipelineVisibility.PRIVATE);
    existing.setExecutionMode(PipelineExecutionMode.ASYNC);
    existing.setStatus(PipelineStatus.DRAFT);
    existing.setVersion(1);
    when(pipelineRepository.findFilteredById("p1")).thenReturn(java.util.Optional.of(existing));
    when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(inv -> inv.getArgument(0));

    PipelineResponse response = pipelineService.archive("p1");

    assertThat(response.status()).isEqualTo(PipelineStatus.ARCHIVED);
    assertThat(response.version()).isEqualTo(2);
    verify(hibernateFilter).setParameter(anyString(), any());
  }
}
