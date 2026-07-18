package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.api.common.DualConfigSupport;
import com.dashpipe.api.connector.CreateConnectorRequest;
import com.dashpipe.api.connector.TenantConnectorResponse;
import com.dashpipe.api.connector.TenantConnectorService;
import com.dashpipe.api.service.CreateTenantServiceRequest;
import com.dashpipe.api.service.TenantServiceConfigService;
import com.dashpipe.api.service.TenantServiceResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineBundleService {

  private final PipelineService pipelineService;
  private final PipelineStepsService pipelineStepsService;
  private final TenantConnectorService tenantConnectorService;
  private final TenantServiceConfigService tenantServiceConfigService;
  private final ObjectMapper objectMapper;

  public PipelineBundleService(
      PipelineService pipelineService,
      PipelineStepsService pipelineStepsService,
      TenantConnectorService tenantConnectorService,
      TenantServiceConfigService tenantServiceConfigService,
      ObjectMapper objectMapper) {
    this.pipelineService = pipelineService;
    this.pipelineStepsService = pipelineStepsService;
    this.tenantConnectorService = tenantConnectorService;
    this.tenantServiceConfigService = tenantServiceConfigService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public PipelineBundle export(String pipelineId) {
    PipelineResponse pipeline = pipelineService.get(pipelineId);
    List<PipelineStepResponse> steps =
        pipeline.steps() == null ? List.of() : pipeline.steps();

    Set<String> connectorIds = new HashSet<>();
    Set<String> serviceIds = new HashSet<>();
    for (PipelineStepResponse step : steps) {
      if (step.connectorIds() != null) {
        connectorIds.addAll(step.connectorIds());
      }
      if (step.serviceIds() != null) {
        serviceIds.addAll(step.serviceIds());
      }
    }

    Map<String, String> connectorIdToKey = new LinkedHashMap<>();
    List<PipelineBundleConnector> connectors = new ArrayList<>();
    for (String connectorId : connectorIds) {
      if (connectorId == null || connectorId.isBlank()) {
        continue;
      }
      try {
        TenantConnectorResponse connector = tenantConnectorService.get(connectorId);
        String key = connectorExportKey(connector.connectorTypeId(), connector.name());
        connectorIdToKey.put(connector.id(), key);
        connectors.add(
            new PipelineBundleConnector(
                key,
                connector.connectorTypeId(),
                connector.name(),
                connector.deploymentConfig(),
                connector.executionConfig() != null
                    ? connector.executionConfig()
                    : connector.config()));
      } catch (Exception ignored) {
        // skip missing connectors
      }
    }

    Map<String, String> serviceIdToKey = new LinkedHashMap<>();
    List<PipelineBundleServiceEntry> services = new ArrayList<>();
    for (String serviceId : serviceIds) {
      if (serviceId == null || serviceId.isBlank()) {
        continue;
      }
      try {
        TenantServiceResponse service = tenantServiceConfigService.get(serviceId);
        String key =
            serviceExportKey(service.serviceTypeId(), service.vendor(), service.name());
        serviceIdToKey.put(service.id(), key);
        services.add(
            new PipelineBundleServiceEntry(
                key,
                service.serviceTypeId(),
                service.vendor(),
                service.name(),
                service.inheritsDefault(),
                service.deploymentConfig(),
                service.executionConfig() != null
                    ? service.executionConfig()
                    : service.config()));
      } catch (Exception ignored) {
        // skip missing services
      }
    }

    List<PipelineBundleStep> bundleSteps = new ArrayList<>();
    for (PipelineStepResponse step : steps) {
      List<String> connectorRefs = new ArrayList<>();
      if (step.connectorIds() != null) {
        for (String id : step.connectorIds()) {
          String key = connectorIdToKey.get(id);
          if (key != null) {
            connectorRefs.add(key);
          }
        }
      }
      List<String> serviceRefs = new ArrayList<>();
      if (step.serviceIds() != null) {
        for (String id : step.serviceIds()) {
          String key = serviceIdToKey.get(id);
          if (key != null) {
            serviceRefs.add(key);
          }
        }
      }
      JsonNode execution =
          step.executionConfig() != null ? step.executionConfig() : step.config();
      bundleSteps.add(
          new PipelineBundleStep(
              step.pipeletId(),
              step.stepOrder(),
              step.deploymentConfig(),
              execution,
              connectorRefs,
              serviceRefs,
              step.inputQueue(),
              step.outputQueue(),
              step.resourceLimits()));
    }

    return new PipelineBundle(
        PipelineBundle.FORMAT_VERSION,
        Instant.now(),
        new PipelineBundlePipeline(
            pipeline.name(),
            pipeline.description(),
            pipeline.visibility() == null ? PipelineVisibility.PRIVATE : pipeline.visibility(),
            pipeline.executionMode() == null
                ? PipelineExecutionMode.ASYNC
                : pipeline.executionMode(),
            pipeline.deploymentConfig(),
            pipeline.executionConfig()),
        bundleSteps,
        connectors,
        services);
  }

  @Transactional
  public PipelineBundleImportRequest.Result importBundle(PipelineBundleImportRequest request) {
    if (request == null || request.bundle() == null) {
      throw new PipelineValidationException("bundle is required");
    }
    PipelineBundle bundle = request.bundle();
    if (bundle.pipeline() == null) {
      throw new PipelineValidationException("bundle.pipeline is required");
    }
    if (bundle.formatVersion() != null
        && !bundle.formatVersion().isBlank()
        && !"1".equals(bundle.formatVersion().trim())) {
      throw new PipelineValidationException(
          "Unsupported format_version: " + bundle.formatVersion());
    }

    String strategy =
        request.conflictStrategy() == null
            ? "create"
            : request.conflictStrategy().trim().toLowerCase(Locale.ROOT);
    boolean reuse = "reuse".equals(strategy);

    List<String> warnings = new ArrayList<>();
    List<String> createdConnectors = new ArrayList<>();
    List<String> reusedConnectors = new ArrayList<>();
    List<String> createdServices = new ArrayList<>();
    List<String> reusedServices = new ArrayList<>();

    Map<String, String> connectorKeyToId = new HashMap<>();
    Map<String, TenantConnectorResponse> existingConnectorsByKey = indexExistingConnectors();
    for (PipelineBundleConnector connector :
        bundle.connectors() == null ? List.<PipelineBundleConnector>of() : bundle.connectors()) {
      String key =
          connector.exportKey() != null && !connector.exportKey().isBlank()
              ? connector.exportKey()
              : connectorExportKey(connector.connectorTypeId(), connector.name());
      if (reuse && existingConnectorsByKey.containsKey(key)) {
        connectorKeyToId.put(key, existingConnectorsByKey.get(key).id());
        reusedConnectors.add(key);
        continue;
      }
      String name = uniqueConnectorName(connector.name());
      if (!Objects.equals(name, connector.name())) {
        warnings.add("Connector renamed on import: " + connector.name() + " → " + name);
      }
      JsonNode execution =
          connector.executionConfig() != null
              ? connector.executionConfig()
              : DualConfigSupport.empty(objectMapper);
      JsonNode deployment =
          connector.deploymentConfig() != null
              ? connector.deploymentConfig()
              : DualConfigSupport.empty(objectMapper);
      TenantConnectorResponse created =
          tenantConnectorService.create(
              new CreateConnectorRequest(
                  connector.connectorTypeId(), name, execution, deployment, execution));
      connectorKeyToId.put(key, created.id());
      connectorKeyToId.put(
          connectorExportKey(connector.connectorTypeId(), name), created.id());
      createdConnectors.add(key);
    }

    Map<String, String> serviceKeyToId = new HashMap<>();
    Map<String, TenantServiceResponse> existingServicesByKey = indexExistingServices();
    for (PipelineBundleServiceEntry service :
        bundle.services() == null
            ? List.<PipelineBundleServiceEntry>of()
            : bundle.services()) {
      String key =
          service.exportKey() != null && !service.exportKey().isBlank()
              ? service.exportKey()
              : serviceExportKey(service.serviceTypeId(), service.vendor(), service.name());
      if (reuse && existingServicesByKey.containsKey(key)) {
        serviceKeyToId.put(key, existingServicesByKey.get(key).id());
        reusedServices.add(key);
        continue;
      }
      String name = uniqueServiceName(service.name());
      if (!Objects.equals(name, service.name())) {
        warnings.add("Service renamed on import: " + service.name() + " → " + name);
      }
      JsonNode execution =
          service.executionConfig() != null
              ? service.executionConfig()
              : DualConfigSupport.empty(objectMapper);
      JsonNode deployment =
          service.deploymentConfig() != null
              ? service.deploymentConfig()
              : DualConfigSupport.empty(objectMapper);
      boolean inheritsDefault =
          service.inheritsDefault() == null || service.inheritsDefault();
      TenantServiceResponse created =
          tenantServiceConfigService.create(
              new CreateTenantServiceRequest(
                  service.serviceTypeId(),
                  service.vendor(),
                  name,
                  execution,
                  deployment,
                  execution,
                  inheritsDefault));
      serviceKeyToId.put(key, created.id());
      serviceKeyToId.put(
          serviceExportKey(service.serviceTypeId(), service.vendor(), name), created.id());
      createdServices.add(key);
    }

    PipelineBundlePipeline pipe = bundle.pipeline();
    String requestedName =
        request.name() != null && !request.name().isBlank()
            ? request.name().trim()
            : (pipe.name() == null ? "Imported pipeline" : pipe.name().trim());
    String pipelineName = uniquePipelineName(requestedName);
    if (!Objects.equals(pipelineName, requestedName)) {
      warnings.add("Pipeline renamed on import: " + requestedName + " → " + pipelineName);
    }

    PipelineResponse createdPipeline =
        pipelineService.create(
            new CreatePipelineRequest(
                pipelineName,
                pipe.description(),
                pipe.visibility(),
                pipe.executionMode(),
                pipe.deploymentConfig(),
                pipe.executionConfig()));

    List<PipelineStepRequest> stepRequests = new ArrayList<>();
    List<PipelineBundleStep> steps =
        bundle.steps() == null
            ? List.of()
            : bundle.steps().stream()
                .sorted((a, b) -> Integer.compare(a.stepOrder(), b.stepOrder()))
                .toList();
    int order = 1;
    for (PipelineBundleStep step : steps) {
      if (step.pipeletId() == null || step.pipeletId().isBlank()) {
        warnings.add("Skipped step with missing pipelet_id at order " + step.stepOrder());
        continue;
      }
      List<String> connectorIds = new ArrayList<>();
      for (String ref :
          step.connectorRefs() == null ? List.<String>of() : step.connectorRefs()) {
        String id = connectorKeyToId.get(ref);
        if (id == null) {
          warnings.add("Unresolved connector_ref: " + ref);
          continue;
        }
        connectorIds.add(id);
      }
      List<String> serviceIds = new ArrayList<>();
      for (String ref :
          step.serviceRefs() == null ? List.<String>of() : step.serviceRefs()) {
        String id = serviceKeyToId.get(ref);
        if (id == null) {
          warnings.add("Unresolved service_ref: " + ref);
          continue;
        }
        serviceIds.add(id);
      }
      JsonNode execution =
          step.executionConfig() != null
              ? step.executionConfig()
              : DualConfigSupport.empty(objectMapper);
      stepRequests.add(
          new PipelineStepRequest(
              step.pipeletId(),
              step.stepOrder() > 0 ? step.stepOrder() : order,
              execution,
              step.deploymentConfig(),
              execution,
              connectorIds,
              serviceIds,
              step.inputQueue(),
              step.outputQueue(),
              step.resourceLimits()));
      order++;
    }

    if (!stepRequests.isEmpty()) {
      pipelineStepsService.replace(
          createdPipeline.id(), new ReplacePipelineStepsRequest(stepRequests));
    } else {
      warnings.add("Imported pipeline has no steps");
    }

    if (containsRedactedSecrets(bundle)) {
      warnings.add(
          "Bundle contains redacted secrets (***). Re-enter secret values on connectors/services before running.");
    }

    return new PipelineBundleImportRequest.Result(
        createdPipeline.id(),
        pipelineName,
        createdConnectors,
        reusedConnectors,
        createdServices,
        reusedServices,
        warnings);
  }

  static String connectorExportKey(String typeId, String name) {
    return safe(typeId) + "::" + safe(name);
  }

  static String serviceExportKey(String typeId, String vendor, String name) {
    return safe(typeId) + "::" + safe(vendor) + "::" + safe(name);
  }

  private static String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private Map<String, TenantConnectorResponse> indexExistingConnectors() {
    Map<String, TenantConnectorResponse> map = new HashMap<>();
    for (TenantConnectorResponse c : tenantConnectorService.list()) {
      map.put(connectorExportKey(c.connectorTypeId(), c.name()), c);
    }
    return map;
  }

  private Map<String, TenantServiceResponse> indexExistingServices() {
    Map<String, TenantServiceResponse> map = new HashMap<>();
    for (TenantServiceResponse s : tenantServiceConfigService.list()) {
      map.put(serviceExportKey(s.serviceTypeId(), s.vendor(), s.name()), s);
    }
    return map;
  }

  private String uniquePipelineName(String base) {
    String candidate = base;
    int i = 2;
    while (nameTakenPipeline(candidate)) {
      candidate = base + " (import " + i + ")";
      if (++i > 100) {
        candidate = base + " (import " + Instant.now().toEpochMilli() + ")";
        break;
      }
    }
    return candidate;
  }

  private boolean nameTakenPipeline(String name) {
    return pipelineService.list().stream()
        .anyMatch(p -> p.name() != null && p.name().equalsIgnoreCase(name));
  }

  private String uniqueConnectorName(String base) {
    String candidate =
        base == null || base.isBlank() ? "Imported connector" : base.trim();
    String original = candidate;
    int i = 2;
    Set<String> names =
        tenantConnectorService.list().stream()
            .map(TenantConnectorResponse::name)
            .filter(Objects::nonNull)
            .map(n -> n.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    while (names.contains(candidate.toLowerCase(Locale.ROOT))) {
      candidate = original + " (import " + i + ")";
      i++;
    }
    return candidate;
  }

  private String uniqueServiceName(String base) {
    String candidate =
        base == null || base.isBlank() ? "Imported service" : base.trim();
    String original = candidate;
    int i = 2;
    Set<String> names =
        tenantServiceConfigService.list().stream()
            .map(TenantServiceResponse::name)
            .filter(Objects::nonNull)
            .map(n -> n.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    while (names.contains(candidate.toLowerCase(Locale.ROOT))) {
      candidate = original + " (import " + i + ")";
      i++;
    }
    return candidate;
  }

  private boolean containsRedactedSecrets(PipelineBundle bundle) {
    List<JsonNode> nodes = new ArrayList<>();
    if (bundle.pipeline() != null) {
      nodes.add(bundle.pipeline().deploymentConfig());
      nodes.add(bundle.pipeline().executionConfig());
    }
    if (bundle.connectors() != null) {
      for (PipelineBundleConnector c : bundle.connectors()) {
        nodes.add(c.deploymentConfig());
        nodes.add(c.executionConfig());
      }
    }
    if (bundle.services() != null) {
      for (PipelineBundleServiceEntry s : bundle.services()) {
        nodes.add(s.deploymentConfig());
        nodes.add(s.executionConfig());
      }
    }
    if (bundle.steps() != null) {
      for (PipelineBundleStep s : bundle.steps()) {
        nodes.add(s.deploymentConfig());
        nodes.add(s.executionConfig());
      }
    }
    for (JsonNode node : nodes) {
      if (node != null && node.isObject() && hasRedacted(node)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasRedacted(JsonNode node) {
    if (node == null || !node.isObject()) {
      return false;
    }
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      JsonNode value = entry.getValue();
      if (value != null && value.isTextual()) {
        String text = value.asText();
        if ("***".equals(text) || "••••••".equals(text)) {
          return true;
        }
      } else if (value != null && value.isObject() && hasRedacted(value)) {
        return true;
      }
    }
    return false;
  }
}
