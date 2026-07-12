package com.dashflow.api.k8s;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dashflow.api.common.DualConfigSupport;
import com.dashflow.api.connector.TenantConnector;
import com.dashflow.api.connector.TenantConnectorRepository;
import com.dashflow.api.pipeline.Pipeline;
import com.dashflow.api.pipeline.PipelineStep;
import com.dashflow.api.service.SecretEncryptor;
import com.dashflow.api.service.TenantServiceConfig;
import com.dashflow.api.service.TenantServiceConfigRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves CONNECTOR_CONFIG / SERVICE_CONFIG / DEPLOYMENT_CONFIG / EXECUTION_CONFIG for a pipeline
 * step Job (mirrors pipelets/_common/config_merge.py layer injection).
 */
@Component
public class PipeletStepEnvResolver {

  private static final Logger log = LoggerFactory.getLogger(PipeletStepEnvResolver.class);

  private final ObjectMapper objectMapper;
  private final TenantConnectorRepository connectorRepository;
  private final TenantServiceConfigRepository serviceRepository;
  private final SecretEncryptor secretEncryptor;

  public PipeletStepEnvResolver(
      ObjectMapper objectMapper,
      TenantConnectorRepository connectorRepository,
      TenantServiceConfigRepository serviceRepository,
      SecretEncryptor secretEncryptor) {
    this.objectMapper = objectMapper;
    this.connectorRepository = connectorRepository;
    this.serviceRepository = serviceRepository;
    this.secretEncryptor = secretEncryptor;
  }

  public PipeletJobEnv resolve(Pipeline pipeline, PipelineStep step) {
    if (pipeline == null || step == null) {
      return PipeletJobEnv.empty();
    }
    String tenantId = pipeline.getTenantId();

    JsonNode connectorJson = mergeConnectors(tenantId, parseIdList(step.getConnectorIds()));
    JsonNode serviceJson = mergeServices(tenantId, parseIdList(step.getServiceIds()));
    JsonNode deploymentJson =
        DualConfigSupport.mergeExtend(
            objectMapper,
            readObject(pipeline.getDeploymentConfig()),
            readObject(step.getDeploymentConfig()));
    JsonNode executionJson = readObject(firstNonBlank(step.getExecutionConfig(), step.getConfig()));

    return new PipeletJobEnv(
        writeJson(secretEncryptor.decryptSecrets(connectorJson)),
        writeJson(secretEncryptor.decryptSecrets(serviceJson)),
        writeJson(deploymentJson),
        writeJson(executionJson));
  }

  private JsonNode mergeConnectors(String tenantId, List<String> ids) {
    ObjectNode merged = objectMapper.createObjectNode();
    for (String id : ids) {
      TenantConnector connector =
          connectorRepository
              .findByIdAndTenantId(id, tenantId)
              .orElse(null);
      if (connector == null) {
        log.warn("Connector {} not found for tenant {} — skipping", id, tenantId);
        continue;
      }
      JsonNode cfg =
          readObject(firstNonBlank(connector.getExecutionConfig(), connector.getConfig()));
      if (cfg.isObject()) {
        cfg.fields().forEachRemaining(e -> merged.set(e.getKey(), e.getValue().deepCopy()));
      }
    }
    return merged;
  }

  private JsonNode mergeServices(String tenantId, List<String> ids) {
    ObjectNode merged = objectMapper.createObjectNode();
    for (String id : ids) {
      TenantServiceConfig service =
          serviceRepository.findByIdAndTenantId(id, tenantId).orElse(null);
      if (service == null) {
        log.warn("Service {} not found for tenant {} — skipping", id, tenantId);
        continue;
      }
      JsonNode cfg =
          readObject(firstNonBlank(service.getExecutionConfig(), service.getTenantConfig()));
      if (cfg.isObject()) {
        cfg.fields().forEachRemaining(e -> merged.set(e.getKey(), e.getValue().deepCopy()));
      }
    }
    return merged;
  }

  private List<String> parseIdList(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    try {
      List<String> ids = objectMapper.readValue(raw, new TypeReference<>() {});
      return ids == null ? List.of() : ids;
    } catch (Exception ex) {
      log.warn("Failed to parse id list JSON: {}", ex.getMessage());
      return List.of();
    }
  }

  private JsonNode readObject(String raw) {
    if (raw == null || raw.isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      JsonNode node = objectMapper.readTree(raw);
      return node != null && node.isObject() ? node : objectMapper.createObjectNode();
    } catch (Exception ex) {
      log.warn("Failed to parse config JSON: {}", ex.getMessage());
      return objectMapper.createObjectNode();
    }
  }

  private String writeJson(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(
          node == null || node.isNull() ? objectMapper.createObjectNode() : node);
    } catch (Exception ex) {
      return "{}";
    }
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a;
    }
    return b;
  }
}
