package com.dashpipe.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class TenantServiceConfigServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ConfigMerger configMerger = new ConfigMerger(objectMapper);
  private final SecretRedactor secretRedactor = new SecretRedactor(objectMapper);

  @Test
  void merge_inheritsDefault() {
    ObjectNode defaults =
        objectMapper
            .createObjectNode()
            .put("issuer", "https://default.example")
            .put("audience", "dashpipe")
            .put("clock_skew_seconds", 300);
    ObjectNode overrides =
        objectMapper.createObjectNode().put("issuer", "https://tenant.example").put("client_id", "abc");

    var merged = configMerger.merge(defaults, overrides, true);

    assertThat(merged.get("issuer").asText()).isEqualTo("https://tenant.example");
    assertThat(merged.get("audience").asText()).isEqualTo("dashpipe");
    assertThat(merged.get("client_id").asText()).isEqualTo("abc");
    assertThat(merged.get("clock_skew_seconds").asInt()).isEqualTo(300);
  }

  @Test
  void merge_withoutInherit_usesOverridesOnly() {
    ObjectNode defaults = objectMapper.createObjectNode().put("issuer", "https://default.example");
    ObjectNode overrides = objectMapper.createObjectNode().put("client_id", "abc");

    var merged = configMerger.merge(defaults, overrides, false);

    assertThat(merged.has("issuer")).isFalse();
    assertThat(merged.get("client_id").asText()).isEqualTo("abc");
  }

  @Test
  void toResponse_redactsSecrets() {
    ObjectNode config =
        objectMapper
            .createObjectNode()
            .put("issuer", "https://auth.example")
            .put("client_secret", "super-secret")
            .put("client_id", "cid");

    var redacted = secretRedactor.redact(config);

    assertThat(redacted.get("issuer").asText()).isEqualTo("https://auth.example");
    assertThat(redacted.get("client_id").asText()).isEqualTo("cid");
    assertThat(redacted.get("client_secret").asText()).isEqualTo(SecretRedactor.REDACTED);
  }
}
