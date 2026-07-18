package com.dashpipe.api;

import com.dashpipe.api.config.IngressProperties;
import com.dashpipe.api.config.ObservabilityPortalProperties;
import com.dashpipe.api.config.PlatformProperties;
import com.dashpipe.api.config.WebhookQueueTriggerProperties;
import com.dashpipe.api.config.WebhookRateLimitProperties;
import com.dashpipe.api.pipeline.PipelineOrchestrationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.dashpipe")
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
