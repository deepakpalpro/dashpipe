package com.dashpipe.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.connector.spi.ServiceResolver;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves merged Auth (and other) service config by explicit {@code tenantId} for public ingress
 * (architecture §9.3 / W3-US02). Does not require {@code X-Tenant-Id}.
 */
@Service
public class DefaultServiceResolver implements ServiceResolver {

  private final TenantServiceConfigRepository repository;
  private final ServiceTypeRepository serviceTypeRepository;
  private final ConfigMerger configMerger;
  private final SecretEncryptor secretEncryptor;
  private final ObjectMapper objectMapper;

  public DefaultServiceResolver(
      TenantServiceConfigRepository repository,
      ServiceTypeRepository serviceTypeRepository,
      ConfigMerger configMerger,
      SecretEncryptor secretEncryptor,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.serviceTypeRepository = serviceTypeRepository;
    this.configMerger = configMerger;
    this.secretEncryptor = secretEncryptor;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public <T> T resolve(String tenantId, String serviceType, String vendor, Class<T> configClass) {
    if (!JsonNode.class.isAssignableFrom(configClass)) {
      throw new IllegalArgumentException("Only JsonNode configClass is supported in Wave 3");
    }

    String typeId = mapServiceTypeId(serviceType);
    String resolvedVendor = vendor == null || vendor.isBlank() ? pickDefaultVendor(typeId) : vendor;

    JsonNode defaults = loadDefaults(typeId, resolvedVendor);
    JsonNode overrides = objectMapper.createObjectNode();
    boolean inherits = true;

    if (tenantId != null && !tenantId.isBlank()) {
      List<TenantServiceConfig> rows =
          repository.findByTenantIdAndServiceTypeIdAndStatus(
              tenantId, typeId, ServiceInstanceStatus.active);
      TenantServiceConfig match =
          rows.stream()
              .filter(r -> resolvedVendor.equals(r.getVendor()))
              .findFirst()
              .orElse(rows.isEmpty() ? null : rows.getFirst());
      if (match != null) {
        overrides = readJson(match.getTenantConfig());
        inherits = match.isInheritsDefault();
        defaults = loadDefaults(match.getServiceTypeId(), match.getVendor());
      }
    }

    JsonNode merged = configMerger.merge(defaults, overrides, inherits);
    JsonNode decrypted = secretEncryptor.decryptSecrets(merged);
    return (T) decrypted;
  }

  private static String mapServiceTypeId(String serviceType) {
    if (serviceType == null || serviceType.isBlank()) {
      throw new IllegalArgumentException("serviceType is required");
    }
    if ("st-auth".equals(serviceType) || "auth".equalsIgnoreCase(serviceType)) {
      return ServiceTypeService.AUTH_TYPE_ID;
    }
    return serviceType;
  }

  private String pickDefaultVendor(String typeId) {
    if (ServiceTypeService.AUTH_TYPE_ID.equals(typeId)) {
      return ServiceTypeService.STUB_AUTH_VENDOR;
    }
    return serviceTypeRepository
        .findByIdWithDefaults(typeId)
        .flatMap(t -> t.getDefaults().stream().findFirst().map(ServiceDefault::getVendor))
        .orElse("default");
  }

  private JsonNode loadDefaults(String serviceTypeId, String vendor) {
    return serviceTypeRepository
        .findByIdWithDefaults(serviceTypeId)
        .flatMap(
            type ->
                type.getDefaults().stream()
                    .filter(d -> d.getVendor().equals(vendor))
                    .findFirst()
                    .map(d -> readJson(d.getDefaultConfig())))
        .orElseGet(objectMapper::createObjectNode);
  }

  private JsonNode readJson(String json) {
    if (json == null || json.isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid service config JSON", ex);
    }
  }
}
