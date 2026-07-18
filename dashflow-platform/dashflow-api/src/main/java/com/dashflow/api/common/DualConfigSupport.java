package com.dashflow.api.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Shared helpers for dual configuration maps:
 * deployment_config (where/how to run) and execution_config (runtime behavior).
 *
 * <p>Merge semantics: defaults first, then overrides/extensions (override keys win; new keys
 * extend).
 */
public final class DualConfigSupport {

  private static final String REDACTED = "***";

  private DualConfigSupport() {}

  /** Shallow merge: {@code overrides} win and may add keys. */
  public static JsonNode mergeExtend(
      ObjectMapper objectMapper, JsonNode defaults, JsonNode overrides) {
    ObjectNode out = objectMapper.createObjectNode();
    if (defaults != null && defaults.isObject()) {
      defaults.fields().forEachRemaining(e -> out.set(e.getKey(), e.getValue().deepCopy()));
    }
    if (overrides != null && overrides.isObject()) {
      overrides.fields().forEachRemaining(e -> out.set(e.getKey(), e.getValue().deepCopy()));
    }
    return out;
  }

  public static JsonNode mergePreservingSecrets(
      ObjectMapper objectMapper, JsonNode existing, JsonNode incoming) {
    if (incoming == null || incoming.isNull() || !incoming.isObject()) {
      return existing == null || existing.isNull()
          ? objectMapper.createObjectNode()
          : existing;
    }
    ObjectNode out = objectMapper.createObjectNode();
    if (existing != null && existing.isObject()) {
      existing.fields().forEachRemaining(e -> out.set(e.getKey(), e.getValue().deepCopy()));
    }
    incoming
        .fields()
        .forEachRemaining(
            e -> {
              String key = e.getKey();
              JsonNode value = e.getValue();
              if (isSecretish(key) && isRedactedPlaceholder(value) && out.has(key)) {
                return;
              }
              out.set(key, value.deepCopy());
            });
    return out;
  }

  public static JsonNode redactForResponse(ObjectMapper objectMapper, JsonNode config) {
    if (config == null || config.isNull() || !config.isObject()) {
      return objectMapper.createObjectNode();
    }
    ObjectNode out = objectMapper.createObjectNode();
    config
        .fields()
        .forEachRemaining(
            e -> {
              if (isSecretish(e.getKey())
                  && e.getValue() != null
                  && e.getValue().isTextual()
                  && !e.getValue().asText().isBlank()) {
                out.put(e.getKey(), REDACTED);
              } else {
                out.set(e.getKey(), e.getValue().deepCopy());
              }
            });
    return out;
  }

  public static JsonNode empty(ObjectMapper objectMapper) {
    return objectMapper.createObjectNode();
  }

  public static JsonNode normalize(ObjectMapper objectMapper, JsonNode node) {
    if (node == null || node.isNull() || !node.isObject()) {
      return empty(objectMapper);
    }
    return node;
  }

  public static boolean isSecretish(String key) {
    if (key == null) {
      return false;
    }
    String k = key.toLowerCase();
    String compact = k.replace("-", "").replace("_", "");
    return compact.contains("secret")
        || compact.contains("password")
        || compact.contains("accesskey")
        || compact.equals("apikey")
        || k.equals("api_key")
        || k.endsWith("_key")
        || (k.endsWith("key") && (compact.contains("access") || compact.contains("api")));
  }

  private static boolean isRedactedPlaceholder(JsonNode value) {
    if (value == null || !value.isTextual()) {
      return false;
    }
    String text = value.asText();
    return "***".equals(text) || "••••••".equals(text);
  }
}
