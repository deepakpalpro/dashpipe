package com.dashpipe.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.api.common.DualConfigSupport;
import com.dashpipe.api.tenant.TenantContext;
import com.dashpipe.api.tenant.TenantContextRequiredException;
import com.dashpipe.api.tenant.TenantFilters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantServiceConfigService {

  private final TenantServiceConfigRepository repository;
  private final ServiceTypeRepository serviceTypeRepository;
  private final ConfigMerger configMerger;
  private final SecretRedactor secretRedactor;
  private final SecretEncryptor secretEncryptor;
  private final ObjectMapper objectMapper;

  @PersistenceContext private EntityManager entityManager;

  public TenantServiceConfigService(
      TenantServiceConfigRepository repository,
      ServiceTypeRepository serviceTypeRepository,
      ConfigMerger configMerger,
      SecretRedactor secretRedactor,
      SecretEncryptor secretEncryptor,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.serviceTypeRepository = serviceTypeRepository;
    this.configMerger = configMerger;
    this.secretRedactor = secretRedactor;
    this.secretEncryptor = secretEncryptor;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public TenantServiceResponse create(CreateTenantServiceRequest request) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    ServiceType type =
        serviceTypeRepository
            .findByIdWithDefaults(request.serviceTypeId())
            .orElseThrow(
                () ->
                    new TenantServiceValidationException(
                        "Unknown service type: " + request.serviceTypeId()));

    boolean vendorOk =
        type.getDefaults().stream().anyMatch(d -> d.getVendor().equals(request.vendor()));
    if (!vendorOk) {
      throw new TenantServiceValidationException(
          "Vendor not registered for type: " + request.vendor());
    }

    String name = request.name().trim();
    if (repository.existsByTenantIdAndName(tenantId, name)) {
      throw new TenantServiceConflictException("Service name already exists: " + name);
    }

    boolean inherits = request.inheritsDefault() == null || request.inheritsDefault();
    JsonNode execution =
        request.executionConfig() != null ? request.executionConfig() : request.tenantConfig();
    JsonNode deployment =
        request.deploymentConfig() != null
            ? request.deploymentConfig()
            : DualConfigSupport.empty(objectMapper);
    JsonNode storedExecution = secretEncryptor.encryptSecrets(execution);
    JsonNode storedDeployment = secretEncryptor.encryptSecrets(deployment);

    TenantServiceConfig entity = new TenantServiceConfig();
    entity.setId(UUID.randomUUID().toString());
    entity.setTenantId(tenantId);
    entity.setServiceTypeId(type.getId());
    entity.setVendor(request.vendor());
    entity.setName(name);
    entity.setTenantConfig(writeJson(storedExecution));
    entity.setExecutionConfig(writeJson(storedExecution));
    entity.setDeploymentConfig(writeJson(storedDeployment));
    entity.setInheritsDefault(inherits);
    entity.setStatus(ServiceInstanceStatus.active);

    return toResponse(repository.save(entity));
  }

  @Transactional(readOnly = true)
  public TenantServiceResponse get(String id) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    return repository
        .findFilteredById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new TenantServiceNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public List<TenantServiceResponse> list() {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    return repository.findAllFiltered().stream().map(this::toResponse).toList();
  }

  @Transactional
  public TenantServiceResponse update(String id, UpdateTenantServiceRequest request) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    TenantServiceConfig entity =
        repository.findFilteredById(id).orElseThrow(() -> new TenantServiceNotFoundException(id));

    entity.setName(request.name().trim());
    if (request.tenantConfig() != null || request.executionConfig() != null) {
      JsonNode incoming =
          request.executionConfig() != null ? request.executionConfig() : request.tenantConfig();
      JsonNode existingExec =
          readJson(
              entity.getExecutionConfig() != null && !entity.getExecutionConfig().isBlank()
                  ? entity.getExecutionConfig()
                  : entity.getTenantConfig());
      JsonNode merged = DualConfigSupport.mergePreservingSecrets(objectMapper, existingExec, incoming);
      JsonNode stored = secretEncryptor.encryptSecrets(merged);
      entity.setTenantConfig(writeJson(stored));
      entity.setExecutionConfig(writeJson(stored));
    }
    if (request.deploymentConfig() != null) {
      JsonNode merged =
          DualConfigSupport.mergePreservingSecrets(
              objectMapper, readJson(entity.getDeploymentConfig()), request.deploymentConfig());
      entity.setDeploymentConfig(writeJson(secretEncryptor.encryptSecrets(merged)));
    }
    if (request.inheritsDefault() != null) {
      entity.setInheritsDefault(request.inheritsDefault());
    }
    if (request.status() != null) {
      entity.setStatus(request.status());
    }
    return toResponse(repository.save(entity));
  }

  @Transactional
  public void delete(String id) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    TenantServiceConfig entity =
        repository.findFilteredById(id).orElseThrow(() -> new TenantServiceNotFoundException(id));
    repository.delete(entity);
  }

  TenantServiceResponse toResponse(TenantServiceConfig entity) {
    ServiceDefault defaultsRow = loadDefaultsRow(entity.getServiceTypeId(), entity.getVendor());
    JsonNode executionDefaults =
        defaultsRow == null
            ? DualConfigSupport.empty(objectMapper)
            : readJson(
                defaultsRow.getDefaultExecutionConfig() != null
                        && !defaultsRow.getDefaultExecutionConfig().isBlank()
                    ? defaultsRow.getDefaultExecutionConfig()
                    : defaultsRow.getDefaultConfig());
    JsonNode deploymentDefaults =
        defaultsRow == null
            ? DualConfigSupport.empty(objectMapper)
            : readJson(defaultsRow.getDefaultDeploymentConfig());

    JsonNode executionOverrides =
        readJson(
            entity.getExecutionConfig() != null && !entity.getExecutionConfig().isBlank()
                ? entity.getExecutionConfig()
                : entity.getTenantConfig());
    JsonNode deploymentOverrides = readJson(entity.getDeploymentConfig());

    JsonNode executionMerged =
        configMerger.merge(executionDefaults, executionOverrides, entity.isInheritsDefault());
    JsonNode deploymentMerged =
        DualConfigSupport.mergeExtend(objectMapper, deploymentDefaults, deploymentOverrides);

    JsonNode redactedExecution = secretRedactor.redact(executionMerged);
    JsonNode redactedDeployment = secretRedactor.redact(deploymentMerged);
    return new TenantServiceResponse(
        entity.getId(),
        entity.getTenantId(),
        entity.getServiceTypeId(),
        entity.getVendor(),
        entity.getName(),
        redactedExecution,
        redactedDeployment,
        redactedExecution,
        entity.isInheritsDefault(),
        entity.getStatus(),
        entity.getCreatedAt());
  }

  private ServiceDefault loadDefaultsRow(String serviceTypeId, String vendor) {
    return serviceTypeRepository
        .findByIdWithDefaults(serviceTypeId)
        .flatMap(
            type ->
                type.getDefaults().stream()
                    .filter(d -> d.getVendor().equals(vendor))
                    .findFirst())
        .orElse(null);
  }

  private JsonNode readJson(String json) {
    if (json == null || json.isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid tenant_config JSON", ex);
    }
  }

  private String writeJson(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(node);
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot serialize tenant_config", ex);
    }
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
