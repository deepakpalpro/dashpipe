-- W1-US02: minimal tenant-owned resource for isolation filters (before full connectors)
CREATE TABLE tenant_notes (
    id          VARCHAR(36)  NOT NULL,
    tenant_id   VARCHAR(36)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_notes_tenant_id (tenant_id),
    CONSTRAINT fk_tenant_notes_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
