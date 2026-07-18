package com.dashflow.api.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deduplicates webhook deliveries (architecture §11.4). Key = {@code X-Webhook-Id} or SHA-256 of
 * raw body, scoped by tenant + connector.
 *
 * <p>TTL: rows store {@code expires_at} = created + {@link #DEFAULT_TTL}; cleanup job deferred —
 * Wave 3 relies on unique constraint + optional future purge of expired rows.
 */
@Service
public class WebhookIdempotencyService {

  public static final String WEBHOOK_ID_HEADER = "X-Webhook-Id";
  public static final Duration DEFAULT_TTL = Duration.ofDays(7);

  private final WebhookIdempotencyKeyRepository repository;

  public WebhookIdempotencyService(WebhookIdempotencyKeyRepository repository) {
    this.repository = repository;
  }

  /** Prefer header; otherwise hash body. Always scoped by tenant+connector at store time. */
  public String extractKey(String webhookIdHeader, byte[] rawBody) {
    if (webhookIdHeader != null && !webhookIdHeader.isBlank()) {
      String trimmed = webhookIdHeader.trim();
      if (trimmed.length() > 128) {
        return trimmed.substring(0, 128);
      }
      return trimmed;
    }
    return "hash:" + sha256Hex(rawBody == null ? new byte[0] : rawBody);
  }

  /**
   * @return existing event id if this key was already claimed; empty if caller should publish with
   *     {@code newEventId} after calling {@link #claim}
   */
  @Transactional(readOnly = true)
  public Optional<String> findExistingEventId(
      String tenantId, String connectorId, String idempotencyKey) {
    return repository
        .findByScopeAndKey(tenantId, connectorId, idempotencyKey)
        .map(WebhookIdempotencyKey::getEventId);
  }

  /**
   * Persist key → eventId. On concurrent duplicate insert, returns the winner's event id instead.
   *
   * @return event id to use (may differ from {@code eventId} if race lost)
   */
  @Transactional
  public String claim(String tenantId, String connectorId, String idempotencyKey, String eventId) {
    Optional<String> existing = findExistingEventId(tenantId, connectorId, idempotencyKey);
    if (existing.isPresent()) {
      return existing.get();
    }

    WebhookIdempotencyKey row = new WebhookIdempotencyKey();
    row.setId(UUID.randomUUID().toString());
    row.setTenantId(tenantId);
    row.setConnectorId(connectorId);
    row.setIdempotencyKey(idempotencyKey);
    row.setEventId(eventId);
    Instant now = Instant.now();
    row.setCreatedAt(now);
    row.setExpiresAt(now.plus(DEFAULT_TTL));

    try {
      repository.saveAndFlush(row);
      return eventId;
    } catch (DataIntegrityViolationException ex) {
      return repository
          .findByScopeAndKey(tenantId, connectorId, idempotencyKey)
          .map(WebhookIdempotencyKey::getEventId)
          .orElseThrow(() -> ex);
    }
  }

  /**
   * Remove a claimed key after a failed publish so the sender can retry without being stuck on a
   * phantom event id (W3-US04).
   */
  @Transactional
  public void release(String tenantId, String connectorId, String idempotencyKey) {
    repository
        .findByScopeAndKey(tenantId, connectorId, idempotencyKey)
        .ifPresent(repository::delete);
  }

  static String sha256Hex(byte[] body) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(body));
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 failed", ex);
    }
  }
}
