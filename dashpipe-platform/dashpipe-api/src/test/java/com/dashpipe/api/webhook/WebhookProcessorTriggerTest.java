package com.dashpipe.api.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dashpipe.api.k8s.PipeletJobClient;
import com.dashpipe.api.k8s.PipeletJobHandle;
import com.dashpipe.api.k8s.PipeletJobRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookProcessorTriggerTest {

  @Mock private PipeletJobClient pipeletJobClient;

  private WebhookProcessorTrigger trigger;

  @BeforeEach
  void setUp() {
    trigger = new WebhookProcessorTrigger(pipeletJobClient);
  }

  @Test
  void depthGreaterThanZero_createsJobOnce() {
    WebhookQueueWatchTarget target =
        new WebhookQueueWatchTarget("T001", "conn-1", "tenant.T001.webhook.conn-1.in");

    org.mockito.Mockito.when(pipeletJobClient.create(any(PipeletJobRequest.class)))
        .thenAnswer(inv -> PipeletJobHandle.stubbed(inv.getArgument(0)));

    assertThat(trigger.onDepth(target, 1)).isTrue();
    assertThat(trigger.onDepth(target, 2)).isFalse();

    verify(pipeletJobClient, times(1)).create(any(PipeletJobRequest.class));
    ArgumentCaptor<PipeletJobRequest> captor = ArgumentCaptor.forClass(PipeletJobRequest.class);
    verify(pipeletJobClient).create(captor.capture());
    assertThat(captor.getValue().inputQueue()).isEqualTo(target.queueName());
    assertThat(captor.getValue().pipeletId())
        .isEqualTo(WebhookProcessorTrigger.WEBHOOK_PROCESSOR_PIPELET_ID);
  }

  @Test
  void depthZero_clearsBusy_allowsRetrigger() {
    WebhookQueueWatchTarget target =
        new WebhookQueueWatchTarget("T001", "conn-1", "tenant.T001.webhook.conn-1.in");

    org.mockito.Mockito.when(pipeletJobClient.create(any(PipeletJobRequest.class)))
        .thenAnswer(inv -> PipeletJobHandle.stubbed(inv.getArgument(0)));

    assertThat(trigger.onDepth(target, 1)).isTrue();
    assertThat(trigger.onDepth(target, 0)).isFalse();
    assertThat(trigger.onDepth(target, 1)).isTrue();

    verify(pipeletJobClient, times(2)).create(any(PipeletJobRequest.class));
  }

  @Test
  void depthZero_doesNotCreate() {
    WebhookQueueWatchTarget target =
        new WebhookQueueWatchTarget("T001", "conn-1", "tenant.T001.webhook.conn-1.in");
    assertThat(trigger.onDepth(target, 0)).isFalse();
    verifyNoInteractions(pipeletJobClient);
  }
}
