package com.dashpipe.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dashpipe.api.billing.QuotaDecision;
import com.dashpipe.api.billing.QuotaService;
import com.dashpipe.api.billing.RunBlockedException;
import com.dashpipe.api.tenant.TenantContext;
import com.dashpipe.api.tenant.TenantFilters;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** W5-US06: quota gate before orchestrator.start. */
@ExtendWith(MockitoExtension.class)
class PipelineRunServiceQuotaGateTest {

  @Mock private PipelineRepository pipelineRepository;
  @Mock private PipelineStepRepository pipelineStepRepository;
  @Mock private PipelineExecutionRepository executionRepository;
  @Mock private PipelineExecutionStepRepository executionStepRepository;
  @Mock private PipelineRunOrchestrator orchestrator;
  @Mock private QuotaService quotaService;
  @Mock private EntityManager entityManager;
  @Mock private Session session;
  @Mock private Filter filter;

  private PipelineRunService service;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId("T001");
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.getEnabledFilter(TenantFilters.NAME)).thenReturn(null);
    when(session.enableFilter(TenantFilters.NAME)).thenReturn(filter);

    service =
        new PipelineRunService(
            pipelineRepository,
            pipelineStepRepository,
            executionRepository,
            executionStepRepository,
            orchestrator,
            quotaService,
            entityManager);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void run_blocksBeforeStart_whenNoCredit() {
    Pipeline pipeline = pipeline("p1");
    when(pipelineRepository.findFilteredById("p1")).thenReturn(Optional.of(pipeline));
    when(quotaService.evaluateTenant("T001"))
        .thenReturn(QuotaDecision.noCredit(BigDecimal.ZERO));

    assertThatThrownBy(() -> service.run("p1"))
        .isInstanceOf(RunBlockedException.class)
        .satisfies(
            ex ->
                assertThat(((RunBlockedException) ex).getDecision().blocksRun()).isTrue());

    verify(orchestrator, never()).start(any(), any(), any());
    verify(pipelineStepRepository, never()).findByPipelineIdOrdered(any());
  }

  @Test
  void run_starts_whenAllowed() {
    Pipeline pipeline = pipeline("p1");
    PipelineStep step = new PipelineStep();
    step.setId("s1");
    PipelineExecution execution = new PipelineExecution();
    execution.setId("e1");
    execution.setPipelineId("p1");
    execution.setTenantId("T001");
    execution.setStatus(ExecutionStatus.RUNNING);

    when(pipelineRepository.findFilteredById("p1")).thenReturn(Optional.of(pipeline));
    when(quotaService.evaluateTenant("T001"))
        .thenReturn(QuotaDecision.allow(new BigDecimal("10.0000")));
    when(pipelineStepRepository.findByPipelineIdOrdered("p1")).thenReturn(List.of(step));
    when(orchestrator.start(
            eq(pipeline), eq(List.of(step)), eq(ExecutionTrigger.MANUAL), isNull()))
        .thenReturn(execution);

    PipelineRunResponse response = service.run("p1");
    assertThat(response.executionId()).isEqualTo("e1");
    verify(orchestrator).start(pipeline, List.of(step), ExecutionTrigger.MANUAL, null);
  }

  private static Pipeline pipeline(String id) {
    Pipeline p = new Pipeline();
    p.setId(id);
    p.setTenantId("T001");
    p.setStatus(PipelineStatus.ACTIVE);
    p.setName("test");
    return p;
  }
}
