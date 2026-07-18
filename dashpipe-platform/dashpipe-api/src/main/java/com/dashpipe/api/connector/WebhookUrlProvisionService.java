package com.dashpipe.api.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.api.config.IngressProperties;
import com.dashpipe.api.service.SecretEncryptor;
import com.dashpipe.api.tenant.TenantContext;
import com.dashpipe.api.tenant.TenantContextRequiredException;
import com.dashpipe.api.tenant.TenantFilters;
import com.dashpipe.api.webhook.WebhookSignatureVerifier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provisions a stable public ingress URL for event_listener connectors (W3-US05). */
@Service
public class WebhookUrlProvisionService {

  public static final String EVENT_LISTENER_TYPE_ID = "ct-event-listener";

  private final TenantConnectorRepository repository;
  private final ConnectorTypeRepository connectorTypeRepository;
  private final IngressProperties ingressProperties;
  private final WebhookSignatureVerifier signatureVerifier;
  private final ObjectMapper objectMapper;

  @PersistenceContext private EntityManager entityManager;

  public WebhookUrlProvisionService(
      TenantConnectorRepository repository,
      ConnectorTypeRepository connectorTypeRepository,
      IngressProperties ingressProperties,
      WebhookSignatureVerifier signatureVerifier,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.connectorTypeRepository = connectorTypeRepository;
    this.ingressProperties = ingressProperties;
    this.signatureVerifier = signatureVerifier;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public WebhookUrlProvisionResponse provision(String connectorId) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    TenantConnector connector =
        repository
            .findFilteredById(connectorId)
            .orElseThrow(() -> new TenantConnectorNotFoundException(connectorId));

    ConnectorType type =
        connectorTypeRepository
            .findById(connector.getConnectorTypeId())
            .orElseThrow(
                () ->
                    new TenantConnectorValidationException(
                        "Unknown connector type: " + connector.getConnectorTypeId()));

    if (type.getType() != ConnectorKind.event_listener) {
      throw new TenantConnectorValidationException(
          "Webhook URL provisioning is only supported for event_listener connectors");
    }

    String webhookUrl =
        ingressProperties.normalizedBaseUrl()
            + "/api/v1/webhooks/"
            + connector.getTenantId()
            + "/"
            + connector.getId();

    String signingSecret = encryptedSigningSecret(connector);
    String signatureHeader = signatureVerifier.resolveSignatureHeader(tenantId);

    return new WebhookUrlProvisionResponse(
        webhookUrl, signingSecret, signatureHeader, connector.getCreatedAt());
  }

  private String encryptedSigningSecret(TenantConnector connector) {
    try {
      JsonNode config = objectMapper.readTree(connector.getConfig());
      if (config == null || !config.hasNonNull("signing_secret")) {
        throw new TenantConnectorValidationException(
            "Connector signing_secret is not configured");
      }
      String raw = config.get("signing_secret").asText();
      if (raw == null || raw.isBlank()) {
        throw new TenantConnectorValidationException(
            "Connector signing_secret is not configured");
      }
      if (raw.startsWith(SecretEncryptor.PREFIX)) {
        return raw;
      }
      return SecretEncryptor.PREFIX + raw;
    } catch (TenantConnectorValidationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid connector config JSON", ex);
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
