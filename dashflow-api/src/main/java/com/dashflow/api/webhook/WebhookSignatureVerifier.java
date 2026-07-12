package com.dashflow.api.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dashflow.api.connector.TenantConnector;
import com.dashflow.api.service.SecretEncryptor;
import com.dashflow.api.service.ServiceTypeService;
import com.dashflow.connector.spi.ServiceResolver;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * GitHub-style HMAC-SHA256 verifier (architecture §11.4). Header default {@code
 * X-Hub-Signature-256: sha256=&lt;hex&gt;}.
 */
@Component
public class WebhookSignatureVerifier {

  public static final String DEFAULT_SIGNATURE_HEADER = "X-Hub-Signature-256";

  private final ServiceResolver serviceResolver;
  private final SecretEncryptor secretEncryptor;
  private final ObjectMapper objectMapper;

  public WebhookSignatureVerifier(
      ServiceResolver serviceResolver,
      SecretEncryptor secretEncryptor,
      ObjectMapper objectMapper) {
    this.serviceResolver = serviceResolver;
    this.secretEncryptor = secretEncryptor;
    this.objectMapper = objectMapper;
  }

  public void verifyOrThrow(
      String tenantId, TenantConnector connector, byte[] rawBody, String providedSignatureHeader) {
    String secret = extractSigningSecret(connector);
    if (secret == null || secret.isBlank()) {
      throw new WebhookSignatureRejectedException("Connector signing_secret is not configured");
    }

    String headerName = resolveSignatureHeader(tenantId);
    if (providedSignatureHeader == null || providedSignatureHeader.isBlank()) {
      throw new WebhookSignatureRejectedException("Missing signature header: " + headerName);
    }

    String expected = "sha256=" + hmacSha256Hex(secret, rawBody == null ? new byte[0] : rawBody);
    if (!constantTimeEquals(expected, providedSignatureHeader.trim())) {
      throw new WebhookSignatureRejectedException("Invalid webhook signature");
    }
  }

  public String resolveSignatureHeader(String tenantId) {
    JsonNode auth =
        serviceResolver.resolve(
            tenantId,
            ServiceTypeService.AUTH_TYPE_ID,
            ServiceTypeService.STUB_AUTH_VENDOR,
            JsonNode.class);
    if (auth != null && auth.hasNonNull("signature_header")) {
      String header = auth.get("signature_header").asText();
      if (header != null && !header.isBlank()) {
        return header;
      }
    }
    return DEFAULT_SIGNATURE_HEADER;
  }

  private String extractSigningSecret(TenantConnector connector) {
    try {
      JsonNode config = secretEncryptor.decryptSecrets(objectMapper.readTree(connector.getConfig()));
      if (config != null && config.hasNonNull("signing_secret")) {
        return config.get("signing_secret").asText();
      }
      return null;
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid connector config JSON", ex);
    }
  }

  static String hmacSha256Hex(String secret, byte[] body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(body);
      return HexFormat.of().formatHex(digest);
    } catch (Exception ex) {
      throw new IllegalStateException("HMAC computation failed", ex);
    }
  }

  static boolean constantTimeEquals(String expected, String provided) {
    byte[] a = expected.getBytes(StandardCharsets.UTF_8);
    byte[] b = provided.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(a, b);
  }
}
