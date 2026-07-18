package com.dashpipe.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public record ServiceDefaultResponse(
    String id,
    String vendor,
    String baseServiceClass,
    JsonNode defaultConfig,
    JsonNode configSchema) {

  static ServiceDefaultResponse from(ServiceDefault entity, ObjectMapper mapper) {
    return new ServiceDefaultResponse(
        entity.getId(),
        entity.getVendor(),
        entity.getBaseServiceClass(),
        readTree(mapper, entity.getDefaultConfig()),
        readTree(mapper, entity.getConfigSchema()));
  }

  private static JsonNode readTree(ObjectMapper mapper, String json) {
    if (json == null || json.isBlank()) {
      return mapper.nullNode();
    }
    try {
      return mapper.readTree(json);
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid JSON stored for service default", ex);
    }
  }
}
