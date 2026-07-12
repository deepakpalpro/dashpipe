package com.dashflow.api.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashflow.api.common.DualConfigSupport;
import com.dashflow.api.tenant.TenantContext;
import com.dashflow.api.tenant.TenantContextRequiredException;
import com.dashflow.api.tenant.TenantFilters;
import com.dashflow.connector.ConnectorRegistry;
import com.dashflow.connector.spi.ConnectionTestResult;
import com.dashflow.connector.spi.Connector;
import com.dashflow.connector.spi.ConnectorConfig;
import com.dashflow.connector.spi.ConnectorContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantConnectorService {

  public static final String REST_TYPE_ID = "ct-rest";

  private final TenantConnectorRepository repository;
  private final ConnectorTypeRepository connectorTypeRepository;
  private final ConnectorRegistry connectorRegistry;
  private final ObjectMapper objectMapper;

  @PersistenceContext private EntityManager entityManager;

  public TenantConnectorService(
      TenantConnectorRepository repository,
      ConnectorTypeRepository connectorTypeRepository,
      ConnectorRegistry connectorRegistry,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.connectorTypeRepository = connectorTypeRepository;
    this.connectorRegistry = connectorRegistry;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public TenantConnectorResponse create(CreateConnectorRequest request) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    ConnectorType type =
        connectorTypeRepository
            .findById(request.connectorTypeId())
            .orElseThrow(
                () ->
                    new TenantConnectorValidationException(
                        "Unknown connector type: " + request.connectorTypeId()));

    String name = request.name().trim();
    if (repository.existsByTenantIdAndName(tenantId, name)) {
      throw new TenantConnectorConflictException("Connector name already exists: " + name);
    }

    TenantConnector entity = new TenantConnector();
    entity.setId(UUID.randomUUID().toString());
    entity.setTenantId(tenantId);
    entity.setConnectorTypeId(type.getId());
    entity.setName(name);
    JsonNode execution =
        request.executionConfig() != null ? request.executionConfig() : request.config();
    JsonNode deployment =
        request.deploymentConfig() != null
            ? request.deploymentConfig()
            : DualConfigSupport.empty(objectMapper);
    entity.setConfig(writeJson(execution));
    entity.setExecutionConfig(writeJson(execution));
    entity.setDeploymentConfig(writeJson(deployment));
    entity.setStatus(ConnectorInstanceStatus.active);
    return toResponse(repository.save(entity));
  }

  @Transactional(readOnly = true)
  public TenantConnectorResponse get(String id) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    return repository
        .findFilteredById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new TenantConnectorNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public List<TenantConnectorResponse> list() {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    return repository.findAllFiltered().stream().map(this::toResponse).toList();
  }

  @Transactional
  public TenantConnectorResponse update(String id, UpdateConnectorRequest request) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    TenantConnector entity =
        repository.findFilteredById(id).orElseThrow(() -> new TenantConnectorNotFoundException(id));

    String name = request.name().trim();
    if (!name.equals(entity.getName()) && repository.existsByTenantIdAndName(tenantId, name)) {
      throw new TenantConnectorConflictException("Connector name already exists: " + name);
    }
    entity.setName(name);
    if (request.config() != null || request.executionConfig() != null) {
      JsonNode incoming =
          request.executionConfig() != null ? request.executionConfig() : request.config();
      JsonNode existingExec =
          readJson(
              entity.getExecutionConfig() != null && !entity.getExecutionConfig().isBlank()
                  ? entity.getExecutionConfig()
                  : entity.getConfig());
      JsonNode merged = DualConfigSupport.mergePreservingSecrets(objectMapper, existingExec, incoming);
      entity.setConfig(writeJson(merged));
      entity.setExecutionConfig(writeJson(merged));
    }
    if (request.deploymentConfig() != null) {
      JsonNode merged =
          DualConfigSupport.mergePreservingSecrets(
              objectMapper, readJson(entity.getDeploymentConfig()), request.deploymentConfig());
      entity.setDeploymentConfig(writeJson(merged));
    }
    if (request.status() != null) {
      entity.setStatus(request.status());
    }
    return toResponse(repository.save(entity));
  }

  @Transactional
  public ConnectionTestResponse test(String id) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    TenantConnector entity =
        repository.findFilteredById(id).orElseThrow(() -> new TenantConnectorNotFoundException(id));

    ConnectorType type =
        connectorTypeRepository
            .findById(entity.getConnectorTypeId())
            .orElseThrow(
                () ->
                    new TenantConnectorValidationException(
                        "Unknown connector type: " + entity.getConnectorTypeId()));

    Connector plugin =
        connectorRegistry
            .findByType(type.getType().name())
            .orElseThrow(
                () ->
                    new TenantConnectorValidationException(
                        "No SPI plugin registered for type: " + type.getType()));

    Connector instance = newInstance(plugin);
    instance.configure(
        new ConnectorContext(tenantId, entity.getId(), null, null, null),
        toConnectorConfig(readJson(entity.getConfig())));

    ConnectionTestResult result = instance.testConnection();
    Instant testedAt = Instant.now();
    entity.setLastTestedAt(testedAt);
    if (!result.success()) {
      entity.setStatus(ConnectorInstanceStatus.error);
    } else if (entity.getStatus() == ConnectorInstanceStatus.error) {
      entity.setStatus(ConnectorInstanceStatus.active);
    }
    repository.save(entity);
    instance.close();

    return new ConnectionTestResponse(
        result.success(), result.latencyMs(), result.message(), testedAt);
  }

  private static Connector newInstance(Connector plugin) {
    try {
      return plugin.getClass().getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(
          "Cannot instantiate connector plugin: " + plugin.getClass().getName(), ex);
    }
  }

  private ConnectorConfig toConnectorConfig(JsonNode configJson) {
    Map<String, Object> properties = new HashMap<>();
    Map<String, String> secrets = new HashMap<>();
    if (configJson != null && configJson.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = configJson.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String key = entry.getKey();
        JsonNode value = entry.getValue();
        if (key.toLowerCase().contains("secret") || key.toLowerCase().contains("password")) {
          secrets.put(key, value.isNull() ? null : value.asText());
        } else if (value.isNumber()) {
          properties.put(key, value.numberValue());
        } else if (value.isBoolean()) {
          properties.put(key, value.booleanValue());
        } else if (!value.isNull()) {
          properties.put(key, value.asText());
        }
      }
    }
    return new ConnectorConfig(properties, secrets);
  }

  /**
   * When the UI sends redacted placeholders ({@code ***}) for secret fields, keep the stored value.
   * Non-secret fields and new keys from {@code incoming} win.
   */
  private JsonNode mergePreservingSecrets(JsonNode existing, JsonNode incoming) {
    if (incoming == null || incoming.isNull() || !incoming.isObject()) {
      return existing == null ? objectMapper.createObjectNode() : existing;
    }
    var out = objectMapper.createObjectNode();
    if (existing != null && existing.isObject()) {
      existing
          .fields()
          .forEachRemaining(e -> out.set(e.getKey(), e.getValue().deepCopy()));
    }
    incoming
        .fields()
        .forEachRemaining(
            e -> {
              String key = e.getKey();
              JsonNode value = e.getValue();
              if (isSecretish(key) && isRedactedPlaceholder(value) && out.has(key)) {
                return;
              }
              out.set(key, value.deepCopy());
            });
    return out;
  }

  private static boolean isSecretish(String key) {
    if (key == null) {
      return false;
    }
    String k = key.toLowerCase();
    return k.contains("secret")
        || k.contains("password")
        || k.equals("api_key")
        || k.endsWith("_key");
  }

  private static boolean isRedactedPlaceholder(JsonNode value) {
    if (value == null || !value.isTextual()) {
      return false;
    }
    String text = value.asText();
    return "***".equals(text) || "••••••".equals(text);
  }

  private TenantConnectorResponse toResponse(TenantConnector entity) {
    JsonNode execution =
        DualConfigSupport.redactForResponse(
            objectMapper,
            readJson(
                entity.getExecutionConfig() != null && !entity.getExecutionConfig().isBlank()
                    ? entity.getExecutionConfig()
                    : entity.getConfig()));
    JsonNode deployment =
        DualConfigSupport.redactForResponse(objectMapper, readJson(entity.getDeploymentConfig()));
    return new TenantConnectorResponse(
        entity.getId(),
        entity.getTenantId(),
        entity.getConnectorTypeId(),
        entity.getName(),
        execution,
        deployment,
        execution,
        entity.getStatus(),
        entity.getLastTestedAt(),
        entity.getCreatedAt());
  }

  private JsonNode readJson(String json) {
    if (json == null || json.isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid connector config JSON", ex);
    }
  }

  private String writeJson(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(node);
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot serialize connector config", ex);
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
