package com.dashflow.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/** Merges platform default_config with tenant overrides (tenant wins). */
@Component
public class ConfigMerger {

  private final ObjectMapper objectMapper;

  public ConfigMerger(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public JsonNode merge(JsonNode defaults, JsonNode overrides, boolean inheritsDefault) {
    ObjectNode result = objectMapper.createObjectNode();
    if (inheritsDefault && defaults != null && defaults.isObject()) {
      result.setAll((ObjectNode) defaults);
    }
    if (overrides != null && overrides.isObject()) {
      overrides
          .fields()
          .forEachRemaining(entry -> result.set(entry.getKey(), entry.getValue().deepCopy()));
    }
    return result;
  }
}
