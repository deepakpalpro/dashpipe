package com.dashflow.spi.messaging;

import java.util.List;

/** Broker-agnostic view of a declared pipeline topology. */
public record PipelineTopologyView(
    String tenantId, String pipelineId, String exchangeName, List<StageTopologyView> stages) {

  public record StageTopologyView(
      int stageOrder, String inputQueue, String outputQueue, String dlq, String routingKey) {}
}
