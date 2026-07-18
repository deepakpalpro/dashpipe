package com.dashpipe.api.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Emits architecture §7.3 structured pipeline logs to the console (Logstash JSON via Boot) and
 * indexes them for ELK smoke / later observability REST (W4-US05).
 */
@Component
public class PipelineLogEmitter {

  private static final Logger log = LoggerFactory.getLogger(PipelineLogEmitter.class);

  private final PipelineLogIndexer indexer;

  public PipelineLogEmitter(PipelineLogIndexer indexer) {
    this.indexer = indexer;
  }

  public String emit(PipelineLogDocument document) {
    String index = indexer.index(document);
    putMdc(document);
    try {
      log.info(
          "pipeline_log index={} execution_id={} message={}",
          index,
          document.executionId(),
          document.message());
    } finally {
      clearMdc();
    }
    return index;
  }

  public String emitStageProcessed(
      String tenantId,
      String pipelineId,
      String executionId,
      String pipeletId,
      long recordsIn,
      long recordsOut,
      long durationMs) {
    return emit(
        PipelineLogDocument.info(
            tenantId,
            pipelineId,
            executionId,
            pipeletId,
            "Processed batch of " + recordsIn + " records",
            recordsIn,
            recordsOut,
            durationMs));
  }

  private static void putMdc(PipelineLogDocument document) {
    if (document.tenantId() != null) {
      MDC.put("tenant_id", document.tenantId());
    }
    if (document.pipelineId() != null) {
      MDC.put("pipeline_id", document.pipelineId());
    }
    if (document.executionId() != null) {
      MDC.put("execution_id", document.executionId());
    }
    if (document.pipeletId() != null) {
      MDC.put("pipelet_id", document.pipeletId());
    }
    if (document.podName() != null) {
      MDC.put("pod_name", document.podName());
    }
  }

  private static void clearMdc() {
    MDC.remove("tenant_id");
    MDC.remove("pipeline_id");
    MDC.remove("execution_id");
    MDC.remove("pipelet_id");
    MDC.remove("pod_name");
  }
}
