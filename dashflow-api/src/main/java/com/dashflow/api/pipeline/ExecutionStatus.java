package com.dashflow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExecutionStatus {
  PENDING("pending"),
  RUNNING("running"),
  COMPLETED("completed"),
  FAILED("failed"),
  CANCELLED("cancelled");

  private final String value;

  ExecutionStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static ExecutionStatus fromValue(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    for (ExecutionStatus status : values()) {
      if (status.value.equalsIgnoreCase(raw) || status.name().equalsIgnoreCase(raw)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown execution status: " + raw);
  }
}
