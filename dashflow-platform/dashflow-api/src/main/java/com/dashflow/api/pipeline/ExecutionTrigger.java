package com.dashflow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExecutionTrigger {
  MANUAL("manual"),
  SCHEDULE("schedule"),
  API("api");

  private final String value;

  ExecutionTrigger(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static ExecutionTrigger fromValue(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    for (ExecutionTrigger trigger : values()) {
      if (trigger.value.equalsIgnoreCase(raw) || trigger.name().equalsIgnoreCase(raw)) {
        return trigger;
      }
    }
    throw new IllegalArgumentException("Unknown execution trigger: " + raw);
  }
}
