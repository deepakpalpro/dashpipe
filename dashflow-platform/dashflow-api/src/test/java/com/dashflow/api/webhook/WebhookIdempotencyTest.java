package com.dashflow.api.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashflow.api.connector.TenantConnector;
import com.dashflow.api.connector.TenantConnectorRepository;
import com.dashflow.api.k8s.PipeletJobClient;
import com.dashflow.api.messaging.QueueNaming;
import com.dashflow.api.messaging.WebhookTopology;
import com.dashflow.api.messaging.WebhookTopologyService;
import com.dashflow.api.tenant.Tenant;
import com.dashflow.api.tenant.TenantRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class WebhookIdempotencyTest {

  @Mock private TenantRepository tenantRepository;
  @Mock private TenantConnectorRepository connectorRepository;
  @Mock private WebhookTopologyService webhookTopologyService;
  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private PipeletJobClient pipeletJobClient;
  @Mock private WebhookSignatureVerifier signatureVerifier;
  @Mock private WebhookIdempotencyService idempotencyService;
  @Mock private WebhookQueueWatchRegistry queueWatchRegistry;
  @Mock private com.dashflow.api.usage.UsageEventEmitter usageEventEmitter;

  private WebhookIngressService service;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    service =
        new WebhookIngressService(
            tenantRepository,
            connectorRepository,
            webhookTopologyService,
            rabbitTemplate,
            pipeletJobClient,
            signatureVerifier,
            idempotencyService,
            queueWatchRegistry,
            usageEventEmitter,
            objectMapper);
  }

  @Test
  void duplicate_isNoOpOrSameEvent() {
    String tenantId = "T001";
    String connectorId = "conn-1";
    stubTarget(tenantId, connectorId);
    byte[] body = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);
    String[] claimedEventId = {null};

    when(idempotencyService.extractKey("abc", body)).thenReturn("abc");
    // First accept: miss → claim; second accept: hit with claimed event id (no publish).
    when(idempotencyService.findExistingEventId(tenantId, connectorId, "abc"))
        .thenReturn(Optional.empty())
        .thenAnswer(inv -> Optional.of(claimedEventId[0]));
    when(idempotencyService.claim(eq(tenantId), eq(connectorId), eq("abc"), anyString()))
        .thenAnswer(
            inv -> {
              claimedEventId[0] = inv.getArgument(3);
              return claimedEventId[0];
            });
    when(webhookTopologyService.declare(tenantId, connectorId))
        .thenReturn(
            new WebhookTopology(
                tenantId,
                connectorId,
                QueueNaming.webhookExchange(tenantId),
                QueueNaming.webhookInputQueue(tenantId, connectorId),
                QueueNaming.webhookDlq(tenantId, connectorId),
                QueueNaming.webhookRoutingKey(connectorId)));

    WebhookAcceptResponse first = service.accept(tenantId, connectorId, body, "sig", "abc");
    WebhookAcceptResponse second = service.accept(tenantId, connectorId, body, "sig", "abc");

    assertThat(second.eventId()).isEqualTo(first.eventId());
    verify(rabbitTemplate, times(1))
        .convertAndSend(any(String.class), any(String.class), any(Object.class));
  }

  @Test
  void differentKeys_twoEvents() {
    String tenantId = "T001";
    String connectorId = "conn-1";
    stubTarget(tenantId, connectorId);
    byte[] body = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);

    when(idempotencyService.extractKey(eq("k1"), eq(body))).thenReturn("k1");
    when(idempotencyService.extractKey(eq("k2"), eq(body))).thenReturn("k2");
    when(idempotencyService.findExistingEventId(eq(tenantId), eq(connectorId), anyString()))
        .thenReturn(Optional.empty());
    when(idempotencyService.claim(eq(tenantId), eq(connectorId), anyString(), anyString()))
        .thenAnswer(inv -> inv.getArgument(3));
    when(webhookTopologyService.declare(tenantId, connectorId))
        .thenReturn(
            new WebhookTopology(
                tenantId,
                connectorId,
                QueueNaming.webhookExchange(tenantId),
                QueueNaming.webhookInputQueue(tenantId, connectorId),
                QueueNaming.webhookDlq(tenantId, connectorId),
                QueueNaming.webhookRoutingKey(connectorId)));

    WebhookAcceptResponse a = service.accept(tenantId, connectorId, body, "sig", "k1");
    WebhookAcceptResponse b = service.accept(tenantId, connectorId, body, "sig", "k2");

    assertThat(a.eventId()).isNotEqualTo(b.eventId());
    verify(rabbitTemplate, times(2))
        .convertAndSend(any(String.class), any(String.class), any(Object.class));
  }

  @Test
  void extractKey_prefersHeaderOverHash() {
    WebhookIdempotencyService real = new WebhookIdempotencyService(null);
    assertThat(real.extractKey("hdr-1", "body".getBytes(StandardCharsets.UTF_8))).isEqualTo("hdr-1");
    assertThat(real.extractKey(null, "body".getBytes(StandardCharsets.UTF_8)))
        .startsWith("hash:")
        .hasSize("hash:".length() + 64);
  }

  private void stubTarget(String tenantId, String connectorId) {
    Tenant tenant = new Tenant();
    tenant.setId(tenantId);
    TenantConnector connector = new TenantConnector();
    connector.setId(connectorId);
    connector.setTenantId(tenantId);
    connector.setConfig("{\"signing_secret\":\"s\"}");
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(connectorRepository.findByIdAndTenantId(connectorId, tenantId))
        .thenReturn(Optional.of(connector));
  }
}
