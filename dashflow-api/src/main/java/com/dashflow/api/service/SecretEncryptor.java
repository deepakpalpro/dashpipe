package com.dashflow.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * W1-US04 stub: prefixes secret values with {@code encrypted:} (not real crypto). Replace with KMS
 * / envelope encryption before production.
 */
@Component
public class SecretEncryptor {

  public static final String PREFIX = "encrypted:";

  private final ObjectMapper objectMapper;

  public SecretEncryptor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public JsonNode encryptSecrets(JsonNode config) {
    if (config == null || config.isNull() || !config.isObject()) {
      return config == null ? objectMapper.createObjectNode() : config;
    }
    ObjectNode copy = ((ObjectNode) config).deepCopy();
    copy.fieldNames()
        .forEachRemaining(
            name -> {
              if (SecretRedactor.isSecretKey(name) && copy.get(name).isTextual()) {
                String value = copy.get(name).asText();
                if (!value.startsWith(PREFIX)) {
                  copy.put(name, PREFIX + value);
                }
              }
            });
    return copy;
  }

  /** Strip stub {@code encrypted:} prefix for runtime use (ingress HMAC, connectors). */
  public String decryptValue(String value) {
    if (value == null) {
      return null;
    }
    if (value.startsWith(PREFIX)) {
      return value.substring(PREFIX.length());
    }
    return value;
  }

  public JsonNode decryptSecrets(JsonNode config) {
    if (config == null || config.isNull() || !config.isObject()) {
      return config == null ? objectMapper.createObjectNode() : config;
    }
    ObjectNode copy = ((ObjectNode) config).deepCopy();
    copy.fieldNames()
        .forEachRemaining(
            name -> {
              if (SecretRedactor.isSecretKey(name) && copy.get(name).isTextual()) {
                copy.put(name, decryptValue(copy.get(name).asText()));
              }
            });
    return copy;
  }
}
