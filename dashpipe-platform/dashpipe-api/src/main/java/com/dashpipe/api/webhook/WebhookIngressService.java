package com.dashpipe.api.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashpipe.api.connector.TenantConnector;
import com.dashpipe.api.connector.TenantConnectorRepository;
import com.dashpipe.api.k8s.PipeletJobClient;
import com.dashpipe.api.messaging.QueueNaming;
import com.dashpipe.api.messaging.WebhookTopology;
import com.dashpipe.api.messaging.WebhookTopologyService;
import com.dashpipe.api.tenant.TenantRepository;
import com.dashpipe.api.usage.UsageEventEmitter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookIngressService {

  private final TenantRepository tenantRepository;
  private final TenantConnectorRepository connectorRepository;
  private final WebhookTopologyService webhookTopologyService;
  private final RabbitTemplate rabbitTemplate;
  private final PipeletJobClient pipeletJobClient;
  private final WebhookSignatureVerifier signatureVerifier;
  private final WebhookIdempotencyService idempotencyService;
  private final WebhookQueueWatchRegistry queueWatchRegistry;
  private final UsageEventEmitter usageEventEmitter;
  private final ObjectMapper objectMapper;

  public WebhookIngressService(
      TenantRepository tenantRepository,
      TenantConnectorRepository connectorRepository,
      WebhookTopologyService webhookTopologyService,
      RabbitTemplate rabbitTemplate,
      PipeletJobClient pipeletJobClient,
      WebhookSignatureVerifier signatureVerifier,
      WebhookIdempotencyService idempotencyService,
      WebhookQueueWatchRegistry queueWatchRegistry,
      UsageEventEmitter usageEventEmitter,
      ObjectMapper objectMapper) {
    this.tenantRepository = tenantRepository;
    this.connectorRepository = connectorRepository;
    this.webhookTopologyService = webhookTopologyService;
    this.rabbitTemplate = rabbitTemplate;
    this.pipeletJobClient = pipeletJobClient;
    this.signatureVerifier = signatureVerifier;
    this.idempotencyService = idempotencyService;
    this.queueWatchRegistry = queueWatchRegistry;
    this.usageEventEmitter = usageEventEmitter;
    this.objectMapper = objectMapper;
  }

  /**
   * Accept an external webhook: validate, verify HMAC, rate-limit, idempotency, publish once,
   * return 202. Must not start a pipelet Job (W3-US06).
   */
  @Transactional
  public WebhookAcceptResponse accept(
      String tenantId,
      String connectorId,
      byte[] rawBody,
      String signatureHeaderValue,
      String webhookIdHeader) {
    if (tenantId == null || tenantId.isBlank() || connectorId == null || connectorId.isBlank()) {
      throw new WebhookTargetNotFoundException(tenantId, connectorId);
    }

    tenantRepository
        .findById(tenantId)
        .orElseThrow(() -> new WebhookTargetNotFoundException(tenantId, connectorId));

    TenantConnector connector =
        connectorRepository
            .findByIdAndTenantId(connectorId, tenantId)
            .orElseThrow(() -> new WebhookTargetNotFoundException(tenantId, connectorId));

    byte[] bodyBytes = rawBody == null ? new byte[0] : rawBody;
    signatureVerifier.verifyOrThrow(tenantId, connector, bodyBytes, signatureHeaderValue);

    String idempotencyKey = idempotencyService.extractKey(webhookIdHeader, bodyBytes);
    String queuedTo = QueueNaming.webhookInputQueue(tenantId, connectorId);

    Optional<String> existing =
        idempotencyService.findExistingEventId(tenantId, connectorId, idempotencyKey);
    if (existing.isPresent()) {
      return new WebhookAcceptResponse(true, existing.get(), queuedTo);
    }

    JsonNode body = parseBody(bodyBytes);
    WebhookTopology topology =
        webhookTopologyService.declare(connector.getTenantId(), connector.getId());

    String eventId = UUID.randomUUID().toString();
    String claimed =
        idempotencyService.claim(tenantId, connectorId, idempotencyKey, eventId);
    if (!claimed.equals(eventId)) {
      // Lost race to concurrent duplicate — do not publish again.
      return new WebhookAcceptResponse(true, claimed, queuedTo);
    }

    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("event_id", eventId);
    envelope.put("tenant_id", tenantId);
    envelope.put("connector_id", connectorId);
    envelope.put("payload", body);

    try {
      rabbitTemplate.convertAndSend(topology.exchange(), topology.routingKey(), envelope);
    } catch (RuntimeException ex) {
      idempotencyService.release(tenantId, connectorId, idempotencyKey);
      throw new WebhookBrokerUnavailableException(
          "Failed to publish webhook event to broker", ex);
    }

    // Register for on-demand processor poller (W3-US06). Do not create Jobs here (US01).
    queueWatchRegistry.register(tenantId, connectorId, queuedTo);

    // Meter once per logical event (idempotent replays skip this path) — W3-US07.
    usageEventEmitter.emitWebhookAccepted(tenantId, connectorId, bodyBytes.length);

    if (pipeletJobClient == null) {
      throw new IllegalStateException("PipeletJobClient bean required");
    }

    return new WebhookAcceptResponse(true, eventId, queuedTo);
  }

  private JsonNode parseBody(byte[] bodyBytes) {
    if (bodyBytes.length == 0) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(bodyBytes);
    } catch (Exception ex) {
      return objectMapper.getNodeFactory().textNode(new String(bodyBytes, StandardCharsets.UTF_8));
    }
  }
}
