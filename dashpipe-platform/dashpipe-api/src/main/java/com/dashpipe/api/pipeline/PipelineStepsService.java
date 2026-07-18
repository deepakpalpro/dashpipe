package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.api.common.DualConfigSupport;
import com.dashpipe.api.messaging.QueueNaming;
import com.dashpipe.api.tenant.TenantContext;
import com.dashpipe.api.tenant.TenantContextRequiredException;
import com.dashpipe.api.tenant.TenantFilters;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineStepsService {

  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  private final PipelineRepository pipelineRepository;
  private final PipelineStepRepository pipelineStepRepository;
  private final EntityManager entityManager;
  private final ObjectMapper objectMapper;

  public PipelineStepsService(
      PipelineRepository pipelineRepository,
      PipelineStepRepository pipelineStepRepository,
      EntityManager entityManager,
      ObjectMapper objectMapper) {
    this.pipelineRepository = pipelineRepository;
    this.pipelineStepRepository = pipelineStepRepository;
    this.entityManager = entityManager;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public PipelineResponse replace(String pipelineId, ReplacePipelineStepsRequest request) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    Pipeline pipeline =
        pipelineRepository
            .findFilteredById(pipelineId)
            .orElseThrow(() -> new PipelineNotFoundException(pipelineId));

    List<PipelineStepRequest> steps = request.steps();
    validateStepOrders(steps);

    pipelineStepRepository.deleteByPipelineId(pipelineId);

    List<PipelineStepRequest> ordered =
        steps.stream()
            .sorted(java.util.Comparator.comparingInt(PipelineStepRequest::stepOrder))
            .toList();

    int lastOrder = ordered.get(ordered.size() - 1).stepOrder();
    List<PipelineStep> saved = new ArrayList<>();
    for (PipelineStepRequest stepRequest : ordered) {
      int order = stepRequest.stepOrder();
      String inputQueue =
          blankToNull(stepRequest.inputQueue()) != null
              ? blankToNull(stepRequest.inputQueue())
              : QueueNaming.stageInputQueue(tenantId, pipelineId, order);
      String outputQueue = blankToNull(stepRequest.outputQueue());
      if (outputQueue == null && order < lastOrder) {
        outputQueue = QueueNaming.stageOutputQueue(tenantId, pipelineId, order);
      }

      PipelineStep step = new PipelineStep();
      step.setId(UUID.randomUUID().toString());
      step.setPipelineId(pipelineId);
      step.setPipeletId(stepRequest.pipeletId().trim());
      step.setStepOrder(order);
      JsonNode execution =
          stepRequest.executionConfig() != null
              ? stepRequest.executionConfig()
              : stepRequest.config();
      JsonNode deployment = stepRequest.deploymentConfig();
      step.setConfig(writeJson(execution));
      step.setExecutionConfig(writeJson(execution));
      step.setDeploymentConfig(writeJson(deployment));
      step.setConnectorIds(writeStringList(stepRequest.connectorIds()));
      step.setServiceIds(writeStringList(stepRequest.serviceIds()));
      step.setInputQueue(inputQueue);
      step.setOutputQueue(outputQueue);
      step.setResourceLimits(writeJson(stepRequest.resourceLimits()));
      saved.add(pipelineStepRepository.save(step));
    }

    pipeline.setVersion(pipeline.getVersion() + 1);
    // Saving a non-empty step graph activates the pipeline so Run works without a separate PUT.
    if (pipeline.getStatus() == PipelineStatus.DRAFT) {
      pipeline.setStatus(PipelineStatus.ACTIVE);
    }
    // Schedule Source step cron is canonical; keep schedule_cron + execution_config in sync.
    String cronFromSteps = cronFromScheduleSourceSteps(saved);
    String cronFromPipeline = PipelineSchedulePoller.cronFromJson(pipeline.getExecutionConfig());
    String cron = cronFromSteps != null ? cronFromSteps : cronFromPipeline;
    if (cron != null) {
      pipeline.setScheduleCron(cron);
      try {
        JsonNode existing = readJsonObject(pipeline.getExecutionConfig());
        var out =
            existing != null && existing.isObject()
                ? (com.fasterxml.jackson.databind.node.ObjectNode) existing.deepCopy()
                : objectMapper.createObjectNode();
        out.put("scheduleCron", cron);
        pipeline.setExecutionConfig(objectMapper.writeValueAsString(out));
      } catch (JsonProcessingException ex) {
        // schedule_cron column still updated above
      }
    }
    pipelineRepository.save(pipeline);

    return PipelineResponse.from(
        pipeline,
        toResponses(saved),
        DualConfigSupport.redactForResponse(
            objectMapper, readJsonObject(pipeline.getDeploymentConfig())),
        DualConfigSupport.redactForResponse(
            objectMapper, readJsonObject(pipeline.getExecutionConfig())));
  }

  List<PipelineStepResponse> loadSteps(String pipelineId) {
    return toResponses(pipelineStepRepository.findByPipelineIdOrdered(pipelineId));
  }

  private JsonNode readJsonObject(String json) {
    if (json == null || json.isBlank()) {
      return DualConfigSupport.empty(objectMapper);
    }
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Corrupt pipeline config JSON", ex);
    }
  }

  private void validateStepOrders(List<PipelineStepRequest> steps) {
    Set<Integer> seen = new HashSet<>();
    for (PipelineStepRequest step : steps) {
      if (!seen.add(step.stepOrder())) {
        throw new PipelineValidationException("Duplicate step_order: " + step.stepOrder());
      }
    }
  }

  private String cronFromScheduleSourceSteps(List<PipelineStep> steps) {
    for (PipelineStep step : steps) {
      if (!"plet-schedule-source".equals(step.getPipeletId())) {
        continue;
      }
      String raw =
          step.getExecutionConfig() != null && !step.getExecutionConfig().isBlank()
              ? step.getExecutionConfig()
              : step.getConfig();
      String cron = PipelineSchedulePoller.cronFromJson(raw);
      if (cron != null) {
        return cron;
      }
    }
    return null;
  }

  private List<PipelineStepResponse> toResponses(List<PipelineStep> steps) {
    return steps.stream().map(this::toResponse).toList();
  }

  private PipelineStepResponse toResponse(PipelineStep step) {
    JsonNode execution =
        readJson(
            step.getExecutionConfig() != null && !step.getExecutionConfig().isBlank()
                ? step.getExecutionConfig()
                : step.getConfig());
    JsonNode deployment = readJson(step.getDeploymentConfig());
    return new PipelineStepResponse(
        step.getId(),
        step.getPipeletId(),
        step.getStepOrder(),
        execution,
        deployment,
        execution,
        readStringList(step.getConnectorIds()),
        readStringList(step.getServiceIds()),
        step.getInputQueue(),
        step.getOutputQueue(),
        readJson(step.getResourceLimits()));
  }

  private String writeJson(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(node);
    } catch (JsonProcessingException ex) {
      throw new PipelineValidationException("Invalid JSON payload");
    }
  }

  private String writeStringList(List<String> values) {
    List<String> safe = values == null ? List.of() : values;
    try {
      return objectMapper.writeValueAsString(safe);
    } catch (JsonProcessingException ex) {
      throw new PipelineValidationException("Invalid id list payload");
    }
  }

  private JsonNode readJson(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Corrupt JSON in pipeline_steps", ex);
    }
  }

  private List<String> readStringList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, STRING_LIST);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Corrupt JSON array in pipeline_steps", ex);
    }
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
