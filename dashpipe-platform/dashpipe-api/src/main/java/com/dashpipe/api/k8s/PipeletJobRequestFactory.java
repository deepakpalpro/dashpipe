package com.dashpipe.api.k8s;

import com.dashpipe.api.messaging.QueueNaming;
import com.dashpipe.api.pipeline.Pipeline;
import com.dashpipe.api.pipeline.PipelineIoMode;
import com.dashpipe.api.pipeline.PipelineStep;
import com.dashpipe.api.pipeline.PipeletAmqpUrlFactory;
import org.springframework.stereotype.Component;

/** Builds fully-configured {@link PipeletJobRequest}s for a pipeline stage. */
@Component
public class PipeletJobRequestFactory {

  private final PipeletStepEnvResolver envResolver;
  private final PipeletAmqpUrlFactory amqpUrlFactory;

  public PipeletJobRequestFactory(
      PipeletStepEnvResolver envResolver, PipeletAmqpUrlFactory amqpUrlFactory) {
    this.envResolver = envResolver;
    this.amqpUrlFactory = amqpUrlFactory;
  }

  public PipeletJobRequest build(
      Pipeline pipeline,
      PipelineStep step,
      String executionId,
      int stageCount,
      String ioMode,
      String amqpUrl) {
    if (pipeline == null || step == null) {
      throw new IllegalArgumentException("pipeline and step are required");
    }
    int stageOrder = step.getStepOrder();
    String inputQueue =
        step.getInputQueue() != null && !step.getInputQueue().isBlank()
            ? step.getInputQueue()
            : QueueNaming.stageInputQueue(pipeline.getTenantId(), pipeline.getId(), stageOrder);
    String outputQueue =
        step.getOutputQueue() != null && !step.getOutputQueue().isBlank()
            ? step.getOutputQueue()
            : (stageOrder < stageCount
                ? QueueNaming.stageOutputQueue(
                    pipeline.getTenantId(), pipeline.getId(), stageOrder)
                : null);
    String resolvedIoMode =
        ioMode != null && !ioMode.isBlank()
            ? PipelineIoMode.normalize(ioMode)
            : PipelineIoMode.fromExecutionConfigJson(pipeline.getExecutionConfig());
    String resolvedAmqp =
        amqpUrl != null && !amqpUrl.isBlank() ? amqpUrl : amqpUrlFactory.resolve();

    PipeletJobEnv env = envResolver.resolve(pipeline, step);
    return PipeletJobRequest.of(
            pipeline.getTenantId(),
            pipeline.getId(),
            executionId,
            step.getPipeletId(),
            stageOrder,
            stageCount,
            inputQueue,
            outputQueue,
            resolvedIoMode,
            resolvedAmqp)
        .withEnv(env);
  }
}
