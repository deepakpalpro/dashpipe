-- W2-US01: tenant pipelines (CRUD; steps/run in later stories)
CREATE TABLE pipelines (
    id               VARCHAR(36)  NOT NULL,
    tenant_id        VARCHAR(36)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      TEXT         NULL,
    visibility       ENUM('public', 'private') NOT NULL DEFAULT 'private',
    execution_mode   ENUM('async', 'sync') NOT NULL DEFAULT 'async',
    version          INT          NOT NULL DEFAULT 1,
    status           ENUM('draft', 'active', 'archived') NOT NULL DEFAULT 'draft',
    schedule_cron    VARCHAR(64)  NULL,
    retry_config     JSON         NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pipelines_tenant_name (tenant_id, name),
    KEY idx_pipelines_tenant_id (tenant_id),
    CONSTRAINT fk_pipelines_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
