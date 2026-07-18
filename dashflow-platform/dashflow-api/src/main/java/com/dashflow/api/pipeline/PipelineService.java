package com.dashflow.api.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashflow.api.common.DualConfigSupport;
import com.dashflow.api.tenant.TenantContext;
import com.dashflow.api.tenant.TenantContextRequiredException;
import com.dashflow.api.tenant.TenantFilters;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineService {

  private final PipelineRepository pipelineRepository;
  private final PipelineStepsService pipelineStepsService;
  private final EntityManager entityManager;
  private final ObjectMapper objectMapper;

  public PipelineService(
      PipelineRepository pipelineRepository,
      PipelineStepsService pipelineStepsService,
      EntityManager entityManager,
      ObjectMapper objectMapper) {
    this.pipelineRepository = pipelineRepository;
    this.pipelineStepsService = pipelineStepsService;
    this.entityManager = entityManager;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public PipelineResponse create(CreatePipelineRequest request) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    String name = request.name().trim();
    if (pipelineRepository.existsByTenantIdAndName(tenantId, name)) {
      throw new PipelineConflictException("Pipeline name already exists: " + name);
    }

    Pipeline pipeline = new Pipeline();
    pipeline.setId(UUID.randomUUID().toString());
    pipeline.setTenantId(tenantId);
    pipeline.setName(name);
    pipeline.setDescription(blankToNull(request.description()));
    pipeline.setVisibility(
        request.visibility() == null ? PipelineVisibility.PRIVATE : request.visibility());
    pipeline.setExecutionMode(
        request.executionMode() == null ? PipelineExecutionMode.ASYNC : request.executionMode());
    pipeline.setVersion(1);
    pipeline.setStatus(PipelineStatus.DRAFT);
    JsonNode deployment =
        DualConfigSupport.normalize(objectMapper, request.deploymentConfig());
    JsonNode execution = DualConfigSupport.normalize(objectMapper, request.executionConfig());
    pipeline.setDeploymentConfig(writeJson(deployment));
    pipeline.setExecutionConfig(writeJson(execution));
    pipeline.setScheduleCron(extractScheduleCron(execution));

    return toResponse(pipelineRepository.save(pipeline), List.of());
  }

  @Transactional(readOnly = true)
  public PipelineResponse get(String id) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    Pipeline pipeline =
        pipelineRepository
            .findFilteredById(id)
            .orElseThrow(() -> new PipelineNotFoundException(id));
    return toResponse(pipeline, pipelineStepsService.loadSteps(id));
  }

  @Transactional(readOnly = true)
  public List<PipelineResponse> list() {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    return pipelineRepository.findAllFiltered().stream()
        .map(p -> toResponse(p, pipelineStepsService.loadSteps(p.getId())))
        .toList();
  }

  @Transactional
  public PipelineResponse update(String id, UpdatePipelineRequest request) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    Pipeline pipeline =
        pipelineRepository
            .findFilteredById(id)
            .orElseThrow(() -> new PipelineNotFoundException(id));

    String name = request.name().trim();
    if (pipelineRepository.existsByTenantIdAndNameAndIdNot(tenantId, name, id)) {
      throw new PipelineConflictException("Pipeline name already exists: " + name);
    }

    pipeline.setName(name);
    pipeline.setDescription(blankToNull(request.description()));
    if (request.visibility() != null) {
      pipeline.setVisibility(request.visibility());
    }
    if (request.executionMode() != null) {
      pipeline.setExecutionMode(request.executionMode());
    }
    if (request.status() != null) {
      if (request.status() == PipelineStatus.ARCHIVED) {
        throw new PipelineValidationException("Use DELETE to archive a pipeline");
      }
      pipeline.setStatus(request.status());
    }
    if (request.deploymentConfig() != null) {
      JsonNode merged =
          DualConfigSupport.mergePreservingSecrets(
              objectMapper, readJson(pipeline.getDeploymentConfig()), request.deploymentConfig());
      pipeline.setDeploymentConfig(writeJson(merged));
    }
    if (request.executionConfig() != null) {
      JsonNode merged =
          DualConfigSupport.mergePreservingSecrets(
              objectMapper, readJson(pipeline.getExecutionConfig()), request.executionConfig());
      pipeline.setExecutionConfig(writeJson(merged));
      pipeline.setScheduleCron(extractScheduleCron(merged));
    }
    pipeline.setVersion(pipeline.getVersion() + 1);
    Pipeline saved = pipelineRepository.save(pipeline);
    return toResponse(saved, pipelineStepsService.loadSteps(id));
  }

  @Transactional
  public PipelineResponse archive(String id) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    Pipeline pipeline =
        pipelineRepository
            .findFilteredById(id)
            .orElseThrow(() -> new PipelineNotFoundException(id));
    pipeline.setStatus(PipelineStatus.ARCHIVED);
    pipeline.setVersion(pipeline.getVersion() + 1);
    Pipeline saved = pipelineRepository.save(pipeline);
    return toResponse(saved, pipelineStepsService.loadSteps(id));
  }

  PipelineResponse toResponse(Pipeline pipeline, List<PipelineStepResponse> steps) {
    return PipelineResponse.from(
        pipeline,
        steps,
        DualConfigSupport.redactForResponse(
            objectMapper, readJson(pipeline.getDeploymentConfig())),
        DualConfigSupport.redactForResponse(
            objectMapper, readJson(pipeline.getExecutionConfig())));
  }

  private JsonNode readJson(String json) {
    if (json == null || json.isBlank()) {
      return DualConfigSupport.empty(objectMapper);
    }
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Corrupt pipeline config JSON", ex);
    }
  }

  private String writeJson(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(DualConfigSupport.normalize(objectMapper, node));
    } catch (JsonProcessingException ex) {
      throw new PipelineValidationException("Invalid pipeline config JSON");
    }
  }

  /** Reads scheduleCron / schedule_cron / cron from execution_config into schedule_cron. */
  static String extractScheduleCron(JsonNode executionConfig) {
    return PipelineSchedulePoller.cronFromNode(executionConfig);
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
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
