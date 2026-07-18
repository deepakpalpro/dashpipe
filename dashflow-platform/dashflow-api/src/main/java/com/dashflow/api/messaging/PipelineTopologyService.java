package com.dashflow.api.messaging;

import com.dashflow.spi.messaging.MessageBroker;
import com.dashflow.spi.messaging.PipelineTopologyView;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Declares tenant-prefixed pipeline stage destinations with per-stage DLQ (architecture §8.2).
 *
 * <p>Delegates to the active {@link MessageBroker} assembly adapter.
 */
@Service
public class PipelineTopologyService {

  private final MessageBroker messageBroker;

  public PipelineTopologyService(MessageBroker messageBroker) {
    this.messageBroker = messageBroker;
  }

  public PipelineTopology declare(String tenantId, String pipelineId, int stageCount) {
    PipelineTopologyView view =
        messageBroker.declarePipelineTopology(tenantId, pipelineId, stageCount);
    List<PipelineStageTopology> stages = new ArrayList<>();
    for (PipelineTopologyView.StageTopologyView s : view.stages()) {
      stages.add(
          new PipelineStageTopology(
              s.stageOrder(), s.inputQueue(), s.outputQueue(), s.dlq(), s.routingKey()));
    }
    return new PipelineTopology(
        view.tenantId(), view.pipelineId(), view.exchangeName(), List.copyOf(stages));
  }

  /**
   * Drop pending messages from stage input queues (and DLQs) so a new run cannot consume a stale
   * kickoff / prior-stage payload. Safe after {@link #declare}.
   */
  public void purgeStageQueues(PipelineTopology topology) {
    if (topology == null || topology.stages() == null) {
      return;
    }
    List<String> names = new ArrayList<>();
    for (PipelineStageTopology stage : topology.stages()) {
      if (stage.inputQueue() != null) {
        names.add(stage.inputQueue());
      }
      if (stage.dlq() != null) {
        names.add(stage.dlq());
      }
    }
    messageBroker.purgeDestinations(names);
  }
}
