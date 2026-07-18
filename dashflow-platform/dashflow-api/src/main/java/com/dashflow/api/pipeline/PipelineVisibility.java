package com.dashflow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PipelineVisibility {
  PUBLIC("public"),
  PRIVATE("private");

  private final String apiValue;

  PipelineVisibility(String apiValue) {
    this.apiValue = apiValue;
  }

  @JsonValue
  public String apiValue() {
    return apiValue;
  }

  @JsonCreator
  public static PipelineVisibility fromApi(String value) {
    if (value == null || value.isBlank()) {
      return PRIVATE;
    }
    for (PipelineVisibility visibility : values()) {
      if (visibility.apiValue.equalsIgnoreCase(value.trim())) {
        return visibility;
      }
    }
    throw new PipelineValidationException("Unknown visibility: " + value);
  }
}
