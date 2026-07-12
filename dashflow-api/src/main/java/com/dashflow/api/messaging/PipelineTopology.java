package com.dashflow.api.messaging;

import java.util.List;

public record PipelineTopology(
    String tenantId, String pipelineId, String exchange, List<PipelineStageTopology> stages) {}
