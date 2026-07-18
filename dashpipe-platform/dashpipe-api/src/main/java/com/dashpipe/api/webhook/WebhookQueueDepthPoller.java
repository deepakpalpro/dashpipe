package com.dashpipe.api.webhook;

import com.dashpipe.api.config.WebhookQueueTriggerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pipeline Manager-style poller: when webhook {@code .in} depth &gt; 0, trigger processor Job
 * (architecture §11.6). Enabled via {@code pipeline.webhook.queue-trigger.enabled=true}.
 */
@Component
@ConditionalOnProperty(prefix = "pipeline.webhook.queue-trigger", name = "enabled", havingValue = "true")
public class WebhookQueueDepthPoller {

  private static final Logger log = LoggerFactory.getLogger(WebhookQueueDepthPoller.class);

  private final WebhookQueueWatchRegistry registry;
  private final WebhookQueueDepthReader depthReader;
  private final WebhookProcessorTrigger trigger;

  public WebhookQueueDepthPoller(
      WebhookQueueWatchRegistry registry,
      WebhookQueueDepthReader depthReader,
      WebhookProcessorTrigger trigger) {
    this.registry = registry;
    this.depthReader = depthReader;
    this.trigger = trigger;
    log.info("Webhook queue depth poller enabled");
  }

  @Scheduled(fixedDelayString = "${pipeline.webhook.queue-trigger.poll-interval-ms:500}")
  public void poll() {
    for (WebhookQueueWatchTarget target : registry.snapshot()) {
      long depth = depthReader.messageCount(target.queueName());
      trigger.onDepth(target, depth);
    }
  }
}
