package com.dashpipe.api.messaging;

public record PipelineStageTopology(
    int stageOrder,
    String inputQueue,
    String outputQueue,
    String dlq,
    String routingKey) {}
