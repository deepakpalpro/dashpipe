package com.dashflow.api.k8s;

/**
 * Request to spawn a pipelet Job for one pipeline stage (architecture §10.3).
 *
 * @param jobName suggested Job metadata name {@code exec-{executionId}-stage-{stageOrder}}
 * @param namespace suggested namespace {@code tenant-{tenantId}}
 * @param ioMode {@code stdio} or {@code queue} — injected as {@code IO_MODE}
 * @param amqpUrl broker URL for queue mode — injected as {@code AMQP_URL}
 * @param connectorConfig JSON for {@code CONNECTOR_CONFIG}
 * @param serviceConfig JSON for {@code SERVICE_CONFIG}
 * @param deploymentConfig JSON for {@code DEPLOYMENT_CONFIG}
 * @param executionConfig JSON for {@code EXECUTION_CONFIG}
 * @param triggerPayload optional JSON for {@code TRIGGER_PAYLOAD} (manual/API ad-hoc run body)
 */
public record PipeletJobRequest(
    String tenantId,
    String pipelineId,
    String executionId,
    String pipeletId,
    int stageOrder,
    int stageCount,
    String inputQueue,
    String outputQueue,
    String jobName,
    String namespace,
    String ioMode,
    String amqpUrl,
    String connectorConfig,
    String serviceConfig,
    String deploymentConfig,
    String executionConfig,
    String triggerPayload) {

  public PipeletJobRequest withEnv(PipeletJobEnv env) {
    PipeletJobEnv resolved = env == null ? PipeletJobEnv.empty() : env;
    return new PipeletJobRequest(
        tenantId,
        pipelineId,
        executionId,
        pipeletId,
        stageOrder,
        stageCount,
        inputQueue,
        outputQueue,
        jobName,
        namespace,
        ioMode,
        amqpUrl,
        resolved.connectorConfig(),
        resolved.serviceConfig(),
        resolved.deploymentConfig(),
        resolved.executionConfig(),
        triggerPayload);
  }

  public PipeletJobRequest withTriggerPayload(String payload) {
    return new PipeletJobRequest(
        tenantId,
        pipelineId,
        executionId,
        pipeletId,
        stageOrder,
        stageCount,
        inputQueue,
        outputQueue,
        jobName,
        namespace,
        ioMode,
        amqpUrl,
        connectorConfig,
        serviceConfig,
        deploymentConfig,
        executionConfig,
        payload);
  }

  public static PipeletJobRequest of(
      String tenantId,
      String pipelineId,
      String executionId,
      String pipeletId,
      int stageOrder,
      int stageCount,
      String inputQueue,
      String outputQueue) {
    return of(
        tenantId,
        pipelineId,
        executionId,
        pipeletId,
        stageOrder,
        stageCount,
        inputQueue,
        outputQueue,
        "queue",
        null);
  }

  public static PipeletJobRequest of(
      String tenantId,
      String pipelineId,
      String executionId,
      String pipeletId,
      int stageOrder,
      int stageCount,
      String inputQueue,
      String outputQueue,
      String ioMode,
      String amqpUrl) {
    String safeExecution = executionId == null ? "unknown" : executionId;
    String safeTenant = tenantId == null ? "unknown" : tenantId;
    String jobName =
        PipeletK8sProperties.sanitizeJobName("exec-" + safeExecution + "-stage-" + stageOrder);
    String namespace = PipeletK8sProperties.namespaceForTenant(safeTenant);
    return new PipeletJobRequest(
        tenantId,
        pipelineId,
        executionId,
        pipeletId,
        stageOrder,
        stageCount,
        inputQueue,
        outputQueue,
        jobName,
        namespace,
        ioMode == null || ioMode.isBlank() ? "queue" : ioMode,
        amqpUrl,
        null,
        null,
        null,
        null,
        null);
  }
}
