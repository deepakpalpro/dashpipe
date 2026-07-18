package com.dashflow.api.pipeline;

import com.dashflow.api.tenant.TenantContext;
import com.dashflow.api.tenant.TenantContextRequiredException;
import com.dashflow.api.tenant.TenantFilters;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validates a pipeline definition without starting an execution or publishing messages. */
@Service
public class PipelineDryRunService {

  private final PipelineRepository pipelineRepository;
  private final PipelineStepRepository pipelineStepRepository;
  private final EntityManager entityManager;

  public PipelineDryRunService(
      PipelineRepository pipelineRepository,
      PipelineStepRepository pipelineStepRepository,
      EntityManager entityManager) {
    this.pipelineRepository = pipelineRepository;
    this.pipelineStepRepository = pipelineStepRepository;
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public PipelineDryRunResponse dryRun(String pipelineId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    Pipeline pipeline =
        pipelineRepository
            .findFilteredById(pipelineId)
            .orElseThrow(() -> new PipelineNotFoundException(pipelineId));

    List<String> messages = new ArrayList<>();
    if (pipeline.getStatus() == PipelineStatus.ARCHIVED) {
      return PipelineDryRunResponse.invalid(List.of("Pipeline is archived"));
    }

    List<PipelineStep> steps = pipelineStepRepository.findByPipelineIdOrdered(pipelineId);
    if (steps.isEmpty()) {
      return PipelineDryRunResponse.invalid(List.of("Pipeline has no steps configured"));
    }

    Set<Integer> orders = new HashSet<>();
    for (PipelineStep step : steps) {
      if (step.getPipeletId() == null || step.getPipeletId().isBlank()) {
        messages.add("Step " + step.getStepOrder() + " is missing pipelet_id");
      }
      if (!orders.add(step.getStepOrder())) {
        messages.add("Duplicate step_order: " + step.getStepOrder());
      }
    }

    if (!messages.isEmpty()) {
      return PipelineDryRunResponse.invalid(messages);
    }

    messages.add(
        "Dry-run OK: "
            + steps.size()
            + " step(s), status="
            + pipeline.getStatus().apiValue()
            + ", version="
            + pipeline.getVersion());
    return PipelineDryRunResponse.ok(messages);
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
