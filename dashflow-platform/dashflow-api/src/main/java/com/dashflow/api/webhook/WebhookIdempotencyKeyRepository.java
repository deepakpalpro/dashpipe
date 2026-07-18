package com.dashflow.api.webhook;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookIdempotencyKeyRepository
    extends JpaRepository<WebhookIdempotencyKey, String> {

  @Query(
      """
      select k from WebhookIdempotencyKey k
      where k.tenantId = :tenantId
        and k.connectorId = :connectorId
        and k.idempotencyKey = :idempotencyKey
      """)
  Optional<WebhookIdempotencyKey> findByScopeAndKey(
      @Param("tenantId") String tenantId,
      @Param("connectorId") String connectorId,
      @Param("idempotencyKey") String idempotencyKey);
}
