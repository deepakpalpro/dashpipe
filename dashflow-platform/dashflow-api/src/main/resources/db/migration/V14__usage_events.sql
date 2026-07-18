-- W5-US01: durable usage / billing events (architecture §6.2)
CREATE TABLE usage_events (
    id               VARCHAR(36)    NOT NULL,
    tenant_id        VARCHAR(36)    NOT NULL,
    execution_id     VARCHAR(36)    NULL,
    pipeline_id      VARCHAR(36)    NULL,
    pipelet_id       VARCHAR(36)    NULL,
    connector_id     VARCHAR(36)    NULL,
    dimension        VARCHAR(64)    NOT NULL,
    quantity         DECIMAL(18, 6) NOT NULL,
    unit             VARCHAR(32)    NULL,
    metadata         JSON           NULL,
    recorded_at      TIMESTAMP(3)   NOT NULL,
    idempotency_key  VARCHAR(191)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usage_events_idempotency (idempotency_key),
    KEY idx_usage_tenant_recorded (tenant_id, recorded_at),
    KEY idx_usage_tenant_dim_recorded (tenant_id, dimension, recorded_at),
    CONSTRAINT fk_usage_events_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
