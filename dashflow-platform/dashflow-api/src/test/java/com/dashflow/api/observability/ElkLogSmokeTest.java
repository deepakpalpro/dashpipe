package com.dashflow.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** W4-US04: structured log shape + stub indexer query by execution_id. */
class ElkLogSmokeTest {

  private InMemoryPipelineLogIndexer indexer;
  private PipelineLogEmitter emitter;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    indexer = new InMemoryPipelineLogIndexer();
    emitter = new PipelineLogEmitter(indexer);
    mapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @Test
  void document_jsonIncludesExecutionIdAndSection73Fields() throws Exception {
    PipelineLogDocument doc =
        new PipelineLogDocument(
            Instant.parse("2026-07-09T00:05:12.345Z"),
            "INFO",
            "T001",
            "pipe-uuid",
            "exec-fixture-1",
            "plet-uuid",
            "stub-pipelet",
            "Processed batch of 100 records",
            100L,
            98L,
            1234L);

    JsonNode json = mapper.valueToTree(doc);
    assertThat(json.get("execution_id").asText()).isEqualTo("exec-fixture-1");
    assertThat(json.get("tenant_id").asText()).isEqualTo("T001");
    assertThat(json.get("pipeline_id").asText()).isEqualTo("pipe-uuid");
    assertThat(json.get("pipelet_id").asText()).isEqualTo("plet-uuid");
    assertThat(json.get("pod_name").asText()).isEqualTo("stub-pipelet");
    assertThat(json.get("message").asText()).contains("100 records");
    assertThat(json.get("records_in").asLong()).isEqualTo(100L);
    assertThat(json.get("records_out").asLong()).isEqualTo(98L);
  }

  @Test
  void emit_indexesUnderSection73Name_andQueryableByExecutionId() {
    String index =
        emitter.emitStageProcessed(
            "T001", "pipe-1", "exec-fixture-99", "plet-src", 1, 1, 5);

    assertThat(index).startsWith("pipeline-logs-t001-");
    assertThat(indexer.findByExecutionId("exec-fixture-99")).hasSize(1);
    assertThat(indexer.findFirstByExecutionId("exec-fixture-99"))
        .isPresent()
        .get()
        .extracting(PipelineLogDocument::executionId, PipelineLogDocument::message)
        .containsExactly("exec-fixture-99", "Processed batch of 1 records");
    assertThat(indexer.findByIndex(index)).hasSize(1);
  }
}
