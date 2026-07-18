package com.dashflow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PipelineStatus {
  DRAFT("draft"),
  ACTIVE("active"),
  ARCHIVED("archived");

  private final String apiValue;

  PipelineStatus(String apiValue) {
    this.apiValue = apiValue;
  }

  @JsonValue
  public String apiValue() {
    return apiValue;
  }

  @JsonCreator
  public static PipelineStatus fromApi(String value) {
    if (value == null || value.isBlank()) {
      return DRAFT;
    }
    for (PipelineStatus status : values()) {
      if (status.apiValue.equalsIgnoreCase(value.trim())) {
        return status;
      }
    }
    throw new PipelineValidationException("Unknown status: " + value);
  }
}
