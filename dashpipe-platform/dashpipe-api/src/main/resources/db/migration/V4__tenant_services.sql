-- W1-US04: per-tenant service config (Auth pattern); inherits platform defaults
CREATE TABLE services (
    id                VARCHAR(36)  NOT NULL,
    tenant_id         VARCHAR(36)  NOT NULL,
    service_type_id   VARCHAR(36)  NOT NULL,
    vendor            VARCHAR(64)  NOT NULL,
    name              VARCHAR(255) NOT NULL,
    tenant_config     JSON         NOT NULL,
    inherits_default  BOOLEAN      NOT NULL DEFAULT TRUE,
    status            ENUM('active', 'inactive') NOT NULL DEFAULT 'active',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_services_tenant_name (tenant_id, name),
    KEY idx_services_tenant_id (tenant_id),
    CONSTRAINT fk_services_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_services_type
        FOREIGN KEY (service_type_id) REFERENCES service_types (id)
);
