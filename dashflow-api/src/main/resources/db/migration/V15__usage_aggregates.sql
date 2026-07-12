-- W5-US03: hourly (and future) usage rollups (architecture §6.2)
CREATE TABLE usage_aggregates (
    id              VARCHAR(36)    NOT NULL,
    tenant_id       VARCHAR(36)    NOT NULL,
    period_start    TIMESTAMP(3)   NOT NULL,
    period_end      TIMESTAMP(3)   NOT NULL,
    granularity     VARCHAR(16)    NOT NULL,
    dimension       VARCHAR(64)    NOT NULL,
    total_quantity  DECIMAL(18, 6) NOT NULL,
    total_cost      DECIMAL(12, 4) NOT NULL,
    created_at      TIMESTAMP(3)   NOT NULL,
    updated_at      TIMESTAMP(3)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usage_agg_bucket (tenant_id, dimension, granularity, period_start),
    KEY idx_usage_agg_tenant_period (tenant_id, period_start),
    CONSTRAINT fk_usage_aggregates_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
