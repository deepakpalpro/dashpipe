package com.dashflow.api;

import com.dashflow.api.config.IngressProperties;
import com.dashflow.api.config.ObservabilityPortalProperties;
import com.dashflow.api.config.PlatformProperties;
import com.dashflow.api.config.WebhookQueueTriggerProperties;
import com.dashflow.api.config.WebhookRateLimitProperties;
import com.dashflow.api.pipeline.PipelineOrchestrationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.dashflow")
@EnableConfigurationProperties({
  IngressProperties.class,
  ObservabilityPortalProperties.class,
  WebhookQueueTriggerProperties.class,
  WebhookRateLimitProperties.class,
  PipelineOrchestrationProperties.class,
  PlatformProperties.class
})
public class PipelineApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(PipelineApiApplication.class, args);
  }
}
