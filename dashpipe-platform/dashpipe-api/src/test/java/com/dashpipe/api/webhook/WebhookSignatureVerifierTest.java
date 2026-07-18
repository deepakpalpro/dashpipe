package com.dashpipe.api.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dashpipe.api.connector.TenantConnector;
import com.dashpipe.api.service.SecretEncryptor;
import com.dashpipe.connector.spi.ServiceResolver;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookSignatureVerifierTest {

  @Mock private ServiceResolver serviceResolver;
  private SecretEncryptor secretEncryptor;
  private WebhookSignatureVerifier verifier;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    secretEncryptor = new SecretEncryptor(objectMapper);
    verifier = new WebhookSignatureVerifier(serviceResolver, secretEncryptor, objectMapper);
  }

  @Test
  void validSignature_accepts() {
    String secret = "test-secret";
    byte[] body = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
    String header = "sha256=" + WebhookSignatureVerifier.hmacSha256Hex(secret, body);

    TenantConnector connector = connectorWithSecret(secret);
    org.mockito.Mockito.when(
            serviceResolver.resolve(
                org.mockito.ArgumentMatchers.eq("T001"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(com.fasterxml.jackson.databind.JsonNode.class)))
        .thenReturn(objectMapper.createObjectNode().put("signature_header", "X-Hub-Signature-256"));

    verifier.verifyOrThrow("T001", connector, body, header);
  }

  @Test
  void invalidSignature_rejects() {
    TenantConnector connector = connectorWithSecret("test-secret");
    org.mockito.Mockito.when(
            serviceResolver.resolve(
                org.mockito.ArgumentMatchers.eq("T001"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(com.fasterxml.jackson.databind.JsonNode.class)))
        .thenReturn(objectMapper.createObjectNode().put("signature_header", "X-Hub-Signature-256"));

    assertThatThrownBy(
            () ->
                verifier.verifyOrThrow(
                    "T001",
                    connector,
                    "{\"a\":1}".getBytes(StandardCharsets.UTF_8),
                    "sha256=deadbeef"))
        .isInstanceOf(WebhookSignatureRejectedException.class);
  }

  @Test
  void missingSignature_rejects() {
    TenantConnector connector = connectorWithSecret("test-secret");
    org.mockito.Mockito.when(
            serviceResolver.resolve(
                org.mockito.ArgumentMatchers.eq("T001"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(com.fasterxml.jackson.databind.JsonNode.class)))
        .thenReturn(objectMapper.createObjectNode());

    assertThatThrownBy(
            () ->
                verifier.verifyOrThrow(
                    "T001", connector, "{}".getBytes(StandardCharsets.UTF_8), null))
        .isInstanceOf(WebhookSignatureRejectedException.class)
        .hasMessageContaining("Missing signature header");
  }

  @Test
  void encryptedSecretPrefix_isStripped() {
    String secret = "plain-secret";
    byte[] body = "ping".getBytes(StandardCharsets.UTF_8);
    String header = "sha256=" + WebhookSignatureVerifier.hmacSha256Hex(secret, body);
    TenantConnector connector = connectorWithSecret(SecretEncryptor.PREFIX + secret);
    org.mockito.Mockito.when(
            serviceResolver.resolve(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(com.fasterxml.jackson.databind.JsonNode.class)))
        .thenReturn(objectMapper.createObjectNode());

    verifier.verifyOrThrow("T001", connector, body, header);
    assertThat(secretEncryptor.decryptValue(SecretEncryptor.PREFIX + secret)).isEqualTo(secret);
  }

  private TenantConnector connectorWithSecret(String signingSecret) {
    ObjectNode config = objectMapper.createObjectNode().put("signing_secret", signingSecret);
    TenantConnector connector = new TenantConnector();
    connector.setId("conn-1");
    connector.setTenantId("T001");
    try {
      connector.setConfig(objectMapper.writeValueAsString(config));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return connector;
  }
}
