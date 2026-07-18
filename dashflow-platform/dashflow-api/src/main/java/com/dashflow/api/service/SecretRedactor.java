package com.dashflow.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Redacts known secret fields from service config JSON before API responses. */
@Component
public class SecretRedactor {

  public static final String REDACTED = "***";

  private static final Set<String> SECRET_KEYS =
      Set.of(
          "client_secret",
          "signing_secret",
          "api_key",
          "password",
          "secret",
          "private_key",
          "access_token",
          "secretaccesskey",
          "accesskeyid",
          "secret_access_key",
          "access_key_id");

  private final ObjectMapper objectMapper;

  public SecretRedactor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public JsonNode redact(JsonNode config) {
    if (config == null || config.isNull() || !config.isObject()) {
      return config == null ? objectMapper.nullNode() : config;
    }
    ObjectNode copy = ((ObjectNode) config).deepCopy();
    copy.fieldNames()
        .forEachRemaining(
            name -> {
              if (isSecretKey(name)) {
                copy.put(name, REDACTED);
              }
            });
    return copy;
  }

  public static boolean isSecretKey(String name) {
    if (name == null) {
      return false;
    }
    String key = name.toLowerCase(Locale.ROOT);
    return SECRET_KEYS.contains(key) || key.endsWith("_secret") || key.endsWith("_password");
  }
}
