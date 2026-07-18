package com.dashflow.api.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Resolves pipeline-level pipelet I/O mode from {@code execution_config.ioMode}. */
public final class PipelineIoMode {

  public static final String STDIO = "stdio";
  public static final String QUEUE = "queue";
  public static final String DEFAULT = QUEUE;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private PipelineIoMode() {}

  public static String normalize(String raw) {
    if (raw == null || raw.isBlank()) {
      return DEFAULT;
    }
    String value = raw.trim().toLowerCase();
    if (STDIO.equals(value) || QUEUE.equals(value)) {
      return value;
    }
    return DEFAULT;
  }

  /** Reads {@code ioMode} from pipeline execution_config JSON string. */
  public static String fromExecutionConfigJson(String executionConfigJson) {
    if (executionConfigJson == null || executionConfigJson.isBlank()) {
      return DEFAULT;
    }
    try {
      JsonNode root = MAPPER.readTree(executionConfigJson);
      if (root == null || !root.isObject()) {
        return DEFAULT;
      }
      JsonNode mode = root.get("ioMode");
      if (mode == null || mode.isNull()) {
        mode = root.get("io_mode");
      }
      if (mode == null || mode.isNull()) {
        return DEFAULT;
      }
      return normalize(mode.asText());
    } catch (Exception ex) {
      return DEFAULT;
    }
  }

  public static boolean isQueue(String mode) {
    return QUEUE.equals(normalize(mode));
  }

  public static boolean isStdio(String mode) {
    return STDIO.equals(normalize(mode));
  }
}
