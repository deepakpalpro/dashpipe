package com.dashpipe.api.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Merge / redact helpers for pipeline {@code deployment_config} secrets. */
final class DeploymentConfigSupport {

  private static final String REDACTED = "***";

  private DeploymentConfigSupport() {}

  static JsonNode mergePreservingSecrets(
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

  static JsonNode redactForResponse(ObjectMapper objectMapper, JsonNode config) {
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

  static boolean isSecretish(String key) {
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
        || k.endsWith("key") && (compact.contains("access") || compact.contains("api"));
  }

  private static boolean isRedactedPlaceholder(JsonNode value) {
    if (value == null || !value.isTextual()) {
      return false;
    }
    String text = value.asText();
    return "***".equals(text) || "••••••".equals(text);
  }
}
