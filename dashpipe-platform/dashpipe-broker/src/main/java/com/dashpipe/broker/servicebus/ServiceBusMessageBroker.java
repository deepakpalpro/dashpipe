package com.dashpipe.broker.servicebus;

import com.dashpipe.spi.messaging.LogicalDestinations;
import com.dashpipe.spi.messaging.MessageBroker;
import com.dashpipe.spi.messaging.PipelineTopologyView;
import com.dashpipe.spi.messaging.PipelineTopologyView.StageTopologyView;
import com.dashpipe.spi.platform.PlatformAssembly;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Azure Service Bus adapter for the platform stage bus.
 *
 * <p>Maps {@link LogicalDestinations} queue names to Service Bus queues. Topology create uses the
 * Admin client when a connection string is configured; otherwise declares are recorded in-memory
 * for local wiring tests.
 */
@Component
@ConditionalOnProperty(
    prefix = "dashpipe.platform",
    name = "broker",
    havingValue = PlatformAssembly.BROKER_SERVICEBUS)
public class ServiceBusMessageBroker implements MessageBroker {

  private static final Logger log = LoggerFactory.getLogger(ServiceBusMessageBroker.class);

  private final String connectionString;
  private final String fullyQualifiedNamespace;
  private final ConcurrentHashMap<String, Boolean> declared = new ConcurrentHashMap<>();

  public ServiceBusMessageBroker(Environment env) {
    this.connectionString = env.getProperty("dashpipe.platform.servicebus.connection-string", "");
    this.fullyQualifiedNamespace =
        env.getProperty("dashpipe.platform.servicebus.fully-qualified-namespace", "");
  }

  @Override
  public String id() {
    return PlatformAssembly.BROKER_SERVICEBUS;
  }

  @Override
  public PipelineTopologyView declarePipelineTopology(
      String tenantId, String pipelineId, int stageCount) {
    if (stageCount < 1) {
      throw new IllegalArgumentException("stageCount must be >= 1");
    }
    String exchangeName = LogicalDestinations.pipelineExchange(tenantId, pipelineId);
    List<StageTopologyView> stages = new ArrayList<>();
    for (int stage = 1; stage <= stageCount; stage++) {
      String input = LogicalDestinations.stageInputQueue(tenantId, pipelineId, stage);
      String dlq = LogicalDestinations.stageDlq(tenantId, pipelineId, stage);
      String rk = LogicalDestinations.stageRoutingKey(stage);
      String output =
          stage < stageCount
              ? LogicalDestinations.stageOutputQueue(tenantId, pipelineId, stage)
              : null;
      ensureQueue(sanitizeQueueName(input));
      ensureQueue(sanitizeQueueName(dlq));
      stages.add(new StageTopologyView(stage, input, output, dlq, rk));
    }
    log.info(
        "servicebus topology declared tenant={} pipeline={} stages={}",
        tenantId,
        pipelineId,
        stageCount);
    return new PipelineTopologyView(tenantId, pipelineId, exchangeName, List.copyOf(stages));
  }

  @Override
  public void declareWebhookTopology(String tenantId, String connectorId) {
    ensureQueue(
        sanitizeQueueName(LogicalDestinations.webhookInputQueue(tenantId, connectorId)));
    ensureQueue(sanitizeQueueName(LogicalDestinations.webhookDlq(tenantId, connectorId)));
  }

  @Override
  public void purgeDestinations(Collection<String> destinationNames) {
    // Service Bus has no simple purge API equivalent to RabbitMQ; no-op for skeleton.
    // Production follow-up: receive-and-abandon or recreate queues via Admin client.
    log.debug("servicebus purgeDestinations skipped (skeleton) count={}",
        destinationNames == null ? 0 : destinationNames.size());
  }

  @Override
  public void publish(String destination, byte[] body, Map<String, Object> headers) {
    String queue = sanitizeQueueName(destination);
    if (!StringUtils.hasText(connectionString)) {
      log.warn(
          "servicebus publish skipped (no connection-string) queue={} bytes={}",
          queue,
          body == null ? 0 : body.length);
      return;
    }
    // Lazy send with Azure SDK — connection string path for assembly bootstrap.
    try (com.azure.messaging.servicebus.ServiceBusSenderClient sender =
        new com.azure.messaging.servicebus.ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .queueName(queue)
            .buildClient()) {
      com.azure.messaging.servicebus.ServiceBusMessage msg =
          new com.azure.messaging.servicebus.ServiceBusMessage(
              body == null ? new byte[0] : body);
      msg.setContentType("application/json");
      if (headers != null) {
        headers.forEach((k, v) -> msg.getApplicationProperties().put(k, v));
      }
      sender.sendMessage(msg);
    }
  }

  @Override
  public void deadLetter(String dlqDestination, byte[] body, Map<String, Object> headers) {
    publish(dlqDestination, body, headers);
  }

  @Override
  public long queueDepth(String destination) {
    return 0L;
  }

  @Override
  public Map<String, String> pipeletConnectionEnv() {
    Map<String, String> env = new LinkedHashMap<>();
    env.put("IO_BROKER", PlatformAssembly.BROKER_SERVICEBUS);
    if (StringUtils.hasText(connectionString)) {
      env.put("SERVICEBUS_CONNECTION_STRING", connectionString);
    }
    if (StringUtils.hasText(fullyQualifiedNamespace)) {
      env.put("SERVICEBUS_NAMESPACE", fullyQualifiedNamespace);
    }
    return env;
  }

  private void ensureQueue(String queueName) {
    declared.put(queueName, Boolean.TRUE);
    if (!StringUtils.hasText(connectionString)) {
      log.debug("servicebus ensureQueue (no admin yet) name={}", queueName);
      return;
    }
    try {
      var admin = new com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder()
          .connectionString(connectionString)
          .buildClient();
      if (!admin.getQueueExists(queueName)) {
        admin.createQueue(queueName);
        log.info("servicebus queue created name={}", queueName);
      }
    } catch (Exception e) {
      log.warn("servicebus ensureQueue failed name={}: {}", queueName, e.getMessage());
    }
  }

  /**
   * Service Bus queue names: letters, numbers, periods, hyphens, underscores; max 260 chars.
   * Logical names already use dots — keep as-is.
   */
  static String sanitizeQueueName(String logical) {
    if (logical == null || logical.isBlank()) {
      throw new IllegalArgumentException("destination is required");
    }
    if (logical.length() > 260) {
      return logical.substring(0, 260);
    }
    return logical;
  }
}
