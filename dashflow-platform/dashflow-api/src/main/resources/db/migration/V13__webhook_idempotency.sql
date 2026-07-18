-- W3-US03: webhook ingress idempotency keys (TTL cleanup documented; expires_at for future job)
CREATE TABLE webhook_idempotency_keys (
    id               VARCHAR(36)  NOT NULL,
    tenant_id        VARCHAR(36)  NOT NULL,
    connector_id     VARCHAR(36)  NOT NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    event_id         VARCHAR(36)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       TIMESTAMP    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_webhook_idempotency (tenant_id, connector_id, idempotency_key),
    KEY idx_webhook_idempotency_expires (expires_at),
    CONSTRAINT fk_webhook_idempotency_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_webhook_idempotency_connector
        FOREIGN KEY (connector_id) REFERENCES connectors (id)
);
