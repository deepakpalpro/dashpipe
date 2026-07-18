package com.dashpipe.api.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record ConnectorTypeResponse(
    String id,
    String type,
    String displayName,
    JsonNode configSchema,
    String spiClass,
    String spiVersion) {

  static ConnectorTypeResponse from(ConnectorType entity, ObjectMapper objectMapper) {
    return new ConnectorTypeResponse(
        entity.getId(),
        entity.getType().name(),
        entity.getDisplayName(),
        readJson(entity.getConfigSchema(), objectMapper),
        entity.getSpiClass(),
        entity.getSpiVersion());
  }

  private static JsonNode readJson(String json, ObjectMapper objectMapper) {
    if (json == null || json.isBlank()) {
      return objectMapper.nullNode();
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid config_schema JSON", ex);
    }
  }
}
