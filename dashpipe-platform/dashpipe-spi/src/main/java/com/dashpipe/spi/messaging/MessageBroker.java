package com.dashpipe.spi.messaging;

import java.util.Collection;
import java.util.Map;

/**
 * Platform message broker SPI — stage handoff, webhook ingress, usage, DLQ.
 *
 * <p>Logical destination names ({@link LogicalDestinations}) stay stable across assemblies.
 * Adapters map them to RabbitMQ queues, Azure Service Bus queues, etc.
 */
public interface MessageBroker {

  /** Broker technology id, e.g. {@code rabbitmq}, {@code servicebus}. */
  String id();

  PipelineTopologyView declarePipelineTopology(String tenantId, String pipelineId, int stageCount);

  void declareWebhookTopology(String tenantId, String connectorId);

  void purgeDestinations(Collection<String> destinationNames);

  void publish(String destination, byte[] body, Map<String, Object> headers);

  void deadLetter(String dlqDestination, byte[] body, Map<String, Object> headers);

  long queueDepth(String destination);

  /** Env vars injected into pipelet Jobs for queue I/O (AMQP_URL, IO_BROKER, …). */
  Map<String, String> pipeletConnectionEnv();
}
