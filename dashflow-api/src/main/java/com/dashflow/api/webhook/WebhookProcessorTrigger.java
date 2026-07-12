package com.dashflow.api.webhook;

import com.dashflow.api.k8s.PipeletJobClient;
import com.dashflow.api.k8s.PipeletJobRequest;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Coalesces queue-depth signals into at most one Job create while depth stays &gt; 0 (W3-US06).
 *
 * <p>Binding is minimal for Wave 3: synthetic pipeline id {@code webhook-{connectorId}} and pipelet
 * {@code webhook-processor}. Real pipeline_steps binding is deferred.
 */
@Service
public class WebhookProcessorTrigger {

  public static final String WEBHOOK_PROCESSOR_PIPELET_ID = "webhook-processor";

  private static final Logger log = LoggerFactory.getLogger(WebhookProcessorTrigger.class);

  private final PipeletJobClient pipeletJobClient;
  private final Set<String> busyKeys = ConcurrentHashMap.newKeySet();

  public WebhookProcessorTrigger(PipeletJobClient pipeletJobClient) {
    this.pipeletJobClient = pipeletJobClient;
  }

  /**
   * @return true if a new Job create was issued
   */
  public boolean onDepth(WebhookQueueWatchTarget target, long depth) {
    if (target == null) {
      return false;
    }
    String key = WebhookQueueWatchRegistry.key(target.tenantId(), target.connectorId());
    if (depth <= 0) {
      busyKeys.remove(key);
      return false;
    }
    if (!busyKeys.add(key)) {
      return false;
    }

    String pipelineId = "webhook-" + target.connectorId();
    String executionId = "wh-" + UUID.randomUUID();
    PipeletJobRequest request =
        PipeletJobRequest.of(
            target.tenantId(),
            pipelineId,
            executionId,
            WEBHOOK_PROCESSOR_PIPELET_ID,
            1,
            1,
            target.queueName(),
            null);
    pipeletJobClient.create(request);
    log.info(
        "Triggered webhook processor Job tenant={} connector={} queue={} depth={}",
        target.tenantId(),
        target.connectorId(),
        target.queueName(),
        depth);
    return true;
  }

  /** Test helper — clear coalesce state. */
  public void clearBusy() {
    busyKeys.clear();
  }
}
