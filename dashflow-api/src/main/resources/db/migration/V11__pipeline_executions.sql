-- W2-US04: pipeline execution records for async runs
CREATE TABLE pipeline_executions (
    id                 VARCHAR(36)  NOT NULL,
    pipeline_id        VARCHAR(36)  NOT NULL,
    tenant_id          VARCHAR(36)  NOT NULL,
    pipeline_version   INT          NOT NULL,
    status             ENUM('pending', 'running', 'completed', 'failed', 'cancelled')
                       NOT NULL DEFAULT 'pending',
    trigger_type       ENUM('manual', 'schedule', 'api') NOT NULL DEFAULT 'manual',
    started_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at       TIMESTAMP    NULL,
    records_in         BIGINT       NOT NULL DEFAULT 0,
    records_out        BIGINT       NOT NULL DEFAULT 0,
    completeness_pct   DECIMAL(5,2) NULL,
    error_summary      JSON         NULL,
    PRIMARY KEY (id),
    KEY idx_pipeline_executions_pipeline_id (pipeline_id),
    KEY idx_pipeline_executions_tenant_id (tenant_id),
    CONSTRAINT fk_pipeline_executions_pipeline
        FOREIGN KEY (pipeline_id) REFERENCES pipelines (id),
    CONSTRAINT fk_pipeline_executions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
