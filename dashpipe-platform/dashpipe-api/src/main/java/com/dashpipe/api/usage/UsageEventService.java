package com.dashpipe.api.usage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists {@link UsageEvent} rows (architecture §6.2). Idempotent when a key is present/derived. */
@Service
public class UsageEventService {

  private static final Logger log = LoggerFactory.getLogger(UsageEventService.class);

  private final UsageEventRepository repository;

  public UsageEventService(UsageEventRepository repository) {
    this.repository = repository;
  }

  /**
   * Inserts the event. Returns the persisted entity id, or the existing id when the idempotency key
   * already exists (no double-bill).
   */
  @Transactional
  public String persist(UsageEvent event) {
    if (event == null) {
      throw new IllegalArgumentException("event required");
    }
    if (event.tenantId() == null || event.tenantId().isBlank()) {
      throw new IllegalArgumentException("tenantId required");
    }
    if (event.dimension() == null || event.dimension().isBlank()) {
      throw new IllegalArgumentException("dimension required");
    }

    String idempotencyKey =
        event.idempotencyKey() != null && !event.idempotencyKey().isBlank()
            ? event.idempotencyKey()
            : deriveIdempotencyKey(event);

    var existing = repository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      return existing.get().getId();
    }

    UsageEventEntity entity = toEntity(event, idempotencyKey);
    try {
      return repository.save(entity).getId();
    } catch (DataIntegrityViolationException ex) {
      return repository
          .findByIdempotencyKey(idempotencyKey)
          .map(UsageEventEntity::getId)
          .orElseThrow(() -> ex);
    }
  }

  static UsageEventEntity toEntity(UsageEvent event, String idempotencyKey) {
    UsageEventEntity entity = new UsageEventEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setTenantId(event.tenantId());
    entity.setExecutionId(blankToNull(event.executionId()));
    entity.setPipelineId(blankToNull(event.pipelineId()));
    entity.setPipeletId(blankToNull(event.pipeletId()));
    entity.setConnectorId(blankToNull(event.connectorId()));
    entity.setDimension(event.dimension());
    entity.setQuantity(BigDecimal.valueOf(event.amount()).setScale(6, RoundingMode.HALF_UP));
    entity.setUnit(blankToNull(event.unit()));
    entity.setRecordedAt(event.occurredAt() != null ? event.occurredAt() : Instant.now());
    entity.setIdempotencyKey(idempotencyKey);
    return entity;
  }

  static String deriveIdempotencyKey(UsageEvent event) {
    Instant when = event.occurredAt() != null ? event.occurredAt() : Instant.EPOCH;
    String raw =
        String.join(
            "|",
            nullToEmpty(event.tenantId()),
            nullToEmpty(event.dimension()),
            nullToEmpty(event.connectorId()),
            nullToEmpty(event.pipelineId()),
            nullToEmpty(event.executionId()),
            nullToEmpty(event.pipeletId()),
            Long.toString(when.toEpochMilli()),
            Double.toString(event.amount()));
    return "ue:" + sha256Hex(raw).substring(0, 40);
  }

  private static String sha256Hex(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      log.warn("SHA-256 unavailable; falling back to UUID idempotency");
      return UUID.randomUUID().toString().replace("-", "");
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
