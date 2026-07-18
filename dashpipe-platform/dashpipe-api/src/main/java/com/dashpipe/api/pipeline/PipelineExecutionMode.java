package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PipelineExecutionMode {
  ASYNC("async"),
  SYNC("sync");

  private final String apiValue;

  PipelineExecutionMode(String apiValue) {
    this.apiValue = apiValue;
  }

  @JsonValue
  public String apiValue() {
    return apiValue;
  }

  @JsonCreator
  public static PipelineExecutionMode fromApi(String value) {
    if (value == null || value.isBlank()) {
      return ASYNC;
    }
    for (PipelineExecutionMode mode : values()) {
      if (mode.apiValue.equalsIgnoreCase(value.trim())) {
        return mode;
      }
    }
    throw new PipelineValidationException("Unknown execution_mode: " + value);
  }
}
