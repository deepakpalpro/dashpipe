package com.dashflow.api.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import com.dashflow.api.usage.StubUsageEventCollector;
import com.dashflow.api.usage.UsageEvent;
import com.dashflow.api.usage.UsageEventEmitter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** W3-US07: successful accept meters webhook_events + bytes_in once per logical event. */
@ExtendWith(MockitoExtension.class)
class WebhookMeteringTest {

  @Mock private TenantRepository tenantRepository;
  @Mock private TenantConnectorRepository connectorRepository;
  @Mock private WebhookTopologyService webhookTopologyService;
  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private PipeletJobClient pipeletJobClient;
  @Mock private WebhookSignatureVerifier signatureVerifier;
  @Mock private WebhookIdempotencyService idempotencyService;
  @Mock private WebhookQueueWatchRegistry queueWatchRegistry;

  private StubUsageEventCollector collector;
  private WebhookIngressService service;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    collector = new StubUsageEventCollector();
    UsageEventEmitter emitter = new UsageEventEmitter(collector);
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
            emitter,
            objectMapper);
  }

  @Test
  void accept_emitsWebhookEventsAndBytesIn() {
    String tenantId = "T001";
    String connectorId = "conn-1";
    stubHappyPath(tenantId, connectorId);
    byte[] body = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);

    service.accept(tenantId, connectorId, body, "sig", "meter-1");

    assertThat(collector.getEvents())
        .anySatisfy(
            e -> {
              assertThat(e.dimension()).isEqualTo(UsageEvent.WEBHOOK_EVENTS);
              assertThat(e.amount()).isEqualTo(1.0);
              assertThat(e.tenantId()).isEqualTo(tenantId);
              assertThat(e.connectorId()).isEqualTo(connectorId);
            })
        .anySatisfy(
            e -> {
              assertThat(e.dimension()).isEqualTo(UsageEvent.BYTES_IN);
              assertThat(e.amount()).isEqualTo(body.length);
              assertThat(e.tenantId()).isEqualTo(tenantId);
              assertThat(e.connectorId()).isEqualTo(connectorId);
            });
  }

  @Test
  void reject_doesNotEmit() {
    String tenantId = "T001";
    String connectorId = "conn-1";
    TenantConnector connector = new TenantConnector();
    connector.setId(connectorId);
    connector.setTenantId(tenantId);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(new Tenant()));
    when(connectorRepository.findByIdAndTenantId(connectorId, tenantId))
        .thenReturn(Optional.of(connector));
    doThrow(new WebhookSignatureRejectedException("bad"))
        .when(signatureVerifier)
        .verifyOrThrow(eq(tenantId), eq(connector), any(), any());

    assertThatThrownBy(
            () ->
                service.accept(
                    tenantId, connectorId, "{}".getBytes(StandardCharsets.UTF_8), "bad", null))
        .isInstanceOf(WebhookSignatureRejectedException.class);

    assertThat(collector.getEvents()).isEmpty();
  }

  @Test
  void duplicate_doesNotEmitAgain() {
    String tenantId = "T001";
    String connectorId = "conn-1";
    Tenant tenant = new Tenant();
    tenant.setId(tenantId);
    TenantConnector connector = new TenantConnector();
    connector.setId(connectorId);
    connector.setTenantId(tenantId);
    connector.setConfig("{\"signing_secret\":\"s\"}");
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(connectorRepository.findByIdAndTenantId(connectorId, tenantId))
        .thenReturn(Optional.of(connector));

    byte[] body = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);
    String[] claimed = {null};

    when(idempotencyService.extractKey("dup", body)).thenReturn("dup");
    when(idempotencyService.findExistingEventId(tenantId, connectorId, "dup"))
        .thenReturn(Optional.empty())
        .thenAnswer(inv -> Optional.of(claimed[0]));
    when(idempotencyService.claim(eq(tenantId), eq(connectorId), eq("dup"), anyString()))
        .thenAnswer(
            inv -> {
              claimed[0] = inv.getArgument(3);
              return claimed[0];
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

    service.accept(tenantId, connectorId, body, "sig", "dup");
    int afterFirst = collector.getEvents().size();
    service.accept(tenantId, connectorId, body, "sig", "dup");

    assertThat(afterFirst).isEqualTo(2);
    assertThat(collector.getEvents()).hasSize(2);
  }

  private void stubHappyPath(String tenantId, String connectorId) {
    Tenant tenant = new Tenant();
    tenant.setId(tenantId);
    TenantConnector connector = new TenantConnector();
    connector.setId(connectorId);
    connector.setTenantId(tenantId);
    connector.setConfig("{\"signing_secret\":\"s\"}");
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(connectorRepository.findByIdAndTenantId(connectorId, tenantId))
        .thenReturn(Optional.of(connector));
    when(idempotencyService.extractKey(any(), any())).thenAnswer(inv -> {
      String hdr = inv.getArgument(0);
      return hdr != null ? hdr : "hash:x";
    });
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
  }
}
