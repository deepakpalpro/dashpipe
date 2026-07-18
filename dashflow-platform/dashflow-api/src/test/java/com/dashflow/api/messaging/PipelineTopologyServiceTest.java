package com.dashflow.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dashflow.spi.messaging.MessageBroker;
import com.dashflow.spi.messaging.PipelineTopologyView;
import com.dashflow.spi.messaging.PipelineTopologyView.StageTopologyView;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PipelineTopologyServiceTest {

  @Mock private MessageBroker messageBroker;

  private PipelineTopologyService service;

  @BeforeEach
  void setUp() {
    service = new PipelineTopologyService(messageBroker);
  }

  @Test
  void declare_mapsBrokerTopologyView() {
    when(messageBroker.declarePipelineTopology(eq("T001"), eq("pipe1"), anyInt()))
        .thenReturn(
            new PipelineTopologyView(
                "T001",
                "pipe1",
                "tenant.T001.pipeline.pipe1",
                List.of(
                    new StageTopologyView(
                        1,
                        "tenant.T001.pipeline.pipe1.stage.1.in",
                        "tenant.T001.pipeline.pipe1.stage.2.in",
                        "tenant.T001.pipeline.pipe1.stage.1.dlq",
                        "stage.1"),
                    new StageTopologyView(
                        2,
                        "tenant.T001.pipeline.pipe1.stage.2.in",
                        null,
                        "tenant.T001.pipeline.pipe1.stage.2.dlq",
                        "stage.2"))));

    PipelineTopology topology = service.declare("T001", "pipe1", 2);

    assertThat(topology.exchange()).isEqualTo("tenant.T001.pipeline.pipe1");
    assertThat(topology.stages()).hasSize(2);
    assertThat(topology.stages().get(0).inputQueue())
        .isEqualTo("tenant.T001.pipeline.pipe1.stage.1.in");
    assertThat(topology.stages().get(0).outputQueue())
        .isEqualTo("tenant.T001.pipeline.pipe1.stage.2.in");
    assertThat(topology.stages().get(0).dlq())
        .isEqualTo("tenant.T001.pipeline.pipe1.stage.1.dlq");
    verify(messageBroker).declarePipelineTopology("T001", "pipe1", 2);
  }
}
