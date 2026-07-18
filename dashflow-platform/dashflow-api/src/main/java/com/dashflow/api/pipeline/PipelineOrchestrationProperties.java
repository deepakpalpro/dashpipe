package com.dashflow.api.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pipeline.orchestration")
public class PipelineOrchestrationProperties {

  /**
   * When true (default), {@link StubStageWorker} advances stages for platform status/metrics.
   * Set false for real queue-mode pipelet I/O without stub completion.
   */
  private boolean stubStageWorker = true;

  /** Optional override for pipelet Job {@code AMQP_URL}; otherwise derived from spring.rabbitmq. */
  private String amqpUrl = "";

  public boolean isStubStageWorker() {
    return stubStageWorker;
  }

  public void setStubStageWorker(boolean stubStageWorker) {
    this.stubStageWorker = stubStageWorker;
  }

  public String getAmqpUrl() {
    return amqpUrl;
  }

  public void setAmqpUrl(String amqpUrl) {
    this.amqpUrl = amqpUrl;
  }
}
