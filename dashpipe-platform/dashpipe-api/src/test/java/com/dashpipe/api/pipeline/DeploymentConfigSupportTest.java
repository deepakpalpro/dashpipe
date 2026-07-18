package com.dashpipe.api.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class DeploymentConfigSupportTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void redact_masksAccessKey() {
    ObjectNode config = mapper.createObjectNode();
    config.put("cloud", "aws");
    config.put("region", "us-east-1");
    config.put("accessKey", "AKIASECRET");

    var redacted = DeploymentConfigSupport.redactForResponse(mapper, config);
    assertThat(redacted.get("cloud").asText()).isEqualTo("aws");
    assertThat(redacted.get("region").asText()).isEqualTo("us-east-1");
    assertThat(redacted.get("accessKey").asText()).isEqualTo("***");
  }

  @Test
  void merge_keepsSecretWhenPlaceholderSent() throws Exception {
    ObjectNode existing = mapper.createObjectNode();
    existing.put("accessKey", "AKIAREAL");
    existing.put("region", "us-west-2");

    ObjectNode incoming = mapper.createObjectNode();
    incoming.put("accessKey", "***");
    incoming.put("region", "eu-west-1");
    incoming.put("cloud", "aws");

    var merged = DeploymentConfigSupport.mergePreservingSecrets(mapper, existing, incoming);
    assertThat(merged.get("accessKey").asText()).isEqualTo("AKIAREAL");
    assertThat(merged.get("region").asText()).isEqualTo("eu-west-1");
    assertThat(merged.get("cloud").asText()).isEqualTo("aws");
  }
}
