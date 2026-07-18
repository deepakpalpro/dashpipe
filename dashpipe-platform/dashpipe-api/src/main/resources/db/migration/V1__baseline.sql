-- W0-US03 baseline: tenants stub aligned with ARCHITECTURE.md §2.2
CREATE TABLE tenants (
    id              VARCHAR(36)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(64)  NOT NULL,
    status          ENUM('active', 'suspended', 'trial') NOT NULL DEFAULT 'trial',
    credit_balance  DECIMAL(12, 4) NOT NULL DEFAULT 0.0000,
    quota_config    JSON NULL,
    k8s_namespace   VARCHAR(63) NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tenants_slug (slug)
);
