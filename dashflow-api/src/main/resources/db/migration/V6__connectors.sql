-- W1-US06: tenant connector instances (Rest test vs WireMock)
CREATE TABLE connectors (
    id                  VARCHAR(36)  NOT NULL,
    tenant_id           VARCHAR(36)  NOT NULL,
    connector_type_id   VARCHAR(36)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    config              JSON         NOT NULL,
    status              ENUM('active', 'inactive', 'error') NOT NULL DEFAULT 'active',
    last_tested_at      TIMESTAMP    NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_connectors_tenant_name (tenant_id, name),
    KEY idx_connectors_tenant_id (tenant_id),
    CONSTRAINT fk_connectors_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_connectors_type
        FOREIGN KEY (connector_type_id) REFERENCES connector_types (id)
);
