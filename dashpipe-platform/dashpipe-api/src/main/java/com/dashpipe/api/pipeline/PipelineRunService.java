package com.dashpipe.api.pipeline;

import com.dashpipe.api.billing.QuotaDecision;
import com.dashpipe.api.billing.QuotaService;
import com.dashpipe.api.billing.RunBlockedException;
import com.dashpipe.api.tenant.TenantContext;
import com.dashpipe.api.tenant.TenantContextRequiredException;
import com.dashpipe.api.tenant.TenantFilters;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineRunService {

  private static final Logger log = LoggerFactory.getLogger(PipelineRunService.class);

  private final PipelineRepository pipelineRepository;
  private final PipelineStepRepository pipelineStepRepository;
  private final PipelineExecutionRepository executionRepository;
  private final PipelineExecutionStepRepository executionStepRepository;
  private final PipelineRunOrchestrator orchestrator;
  private final QuotaService quotaService;
  private final EntityManager entityManager;

  public PipelineRunService(
      PipelineRepository pipelineRepository,
      PipelineStepRepository pipelineStepRepository,
      PipelineExecutionRepository executionRepository,
      PipelineExecutionStepRepository executionStepRepository,
      PipelineRunOrchestrator orchestrator,
      QuotaService quotaService,
      EntityManager entityManager) {
    this.pipelineRepository = pipelineRepository;
    this.pipelineStepRepository = pipelineStepRepository;
    this.executionRepository = executionRepository;
    this.executionStepRepository = executionStepRepository;
    this.orchestrator = orchestrator;
    this.quotaService = quotaService;
    this.entityManager = entityManager;
  }

  @Transactional
  public PipelineRunResponse run(String pipelineId) {
    return run(pipelineId, null);
  }

  @Transactional
  public PipelineRunResponse run(String pipelineId, JsonNode triggerPayload) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    Pipeline pipeline =
        pipelineRepository
            .findFilteredById(pipelineId)
            .orElseThrow(() -> new PipelineNotFoundException(pipelineId));

    QuotaDecision decision = quotaService.evaluateTenant(tenantId);
    if (decision.blocksRun()) {
      throw new RunBlockedException(decision);
    }
    if (!decision.allowed()) {
      // defensive — blocksRun covers HARD/NO_CREDIT; soft is allowed
      throw new RunBlockedException(decision);
    }

    List<PipelineStep> steps = pipelineStepRepository.findByPipelineIdOrdered(pipelineId);
    PipelineExecution execution =
        orchestrator.start(pipeline, steps, ExecutionTrigger.MANUAL, triggerPayload);
    log.debug(
        "pipeline run accepted pipelineId={} executionId={} quota={} hasPayload={}",
        pipelineId,
        execution.getId(),
        decision.code(),
        triggerPayload != null && !triggerPayload.isNull());
    return PipelineRunResponse.from(execution);
  }

  @Transactional(readOnly = true)
  public List<PipelineExecutionResponse> listExecutions(String pipelineId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    pipelineRepository
        .findFilteredById(pipelineId)
        .orElseThrow(() -> new PipelineNotFoundException(pipelineId));

    return executionRepository.findFilteredByPipelineId(pipelineId).stream()
        .map(PipelineExecutionResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public PipelineExecutionResponse getExecution(String pipelineId, String executionId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    pipelineRepository
        .findFilteredById(pipelineId)
        .orElseThrow(() -> new PipelineNotFoundException(pipelineId));

    PipelineExecution execution =
        executionRepository
            .findFilteredById(executionId)
            .filter(e -> e.getPipelineId().equals(pipelineId))
            .orElseThrow(() -> new PipelineExecutionNotFoundException(executionId));
    return PipelineExecutionResponse.from(
        execution, executionStepRepository.findByExecutionIdOrdered(executionId));
  }

  @Transactional
  public PipelineExecutionResponse cancel(String pipelineId, String executionId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    pipelineRepository
        .findFilteredById(pipelineId)
        .orElseThrow(() -> new PipelineNotFoundException(pipelineId));

    PipelineExecution execution =
        executionRepository
            .findFilteredById(executionId)
            .filter(e -> e.getPipelineId().equals(pipelineId))
            .orElseThrow(() -> new PipelineExecutionNotFoundException(executionId));

    int stageCount = pipelineStepRepository.findByPipelineIdOrdered(pipelineId).size();
    PipelineExecution cancelled = orchestrator.cancel(execution, stageCount);
    log.info(
        "pipeline execution force-stopped pipelineId={} executionId={} status={}",
        pipelineId,
        cancelled.getId(),
        cancelled.getStatus());
    return PipelineExecutionResponse.from(
        cancelled, executionStepRepository.findByExecutionIdOrdered(cancelled.getId()));
  }

  private static String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new TenantContextRequiredException();
    }
    return tenantId;
  }

  private void enableTenantFilter(String tenantId) {
    Session session = entityManager.unwrap(Session.class);
    var filter = session.getEnabledFilter(TenantFilters.NAME);
    if (filter == null) {
      filter = session.enableFilter(TenantFilters.NAME);
    }
    filter.setParameter(TenantFilters.PARAM_TENANT_ID, tenantId);
  }
}
