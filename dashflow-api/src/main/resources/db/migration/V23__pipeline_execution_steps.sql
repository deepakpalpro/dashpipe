-- Per-stage (pipelet) status + timing for pipeline executions.
-- One row per pipeline step per execution; populated by the orchestrator on run
-- start and advanced by the stub stage worker / K8s Job poller as stages progress.
CREATE TABLE IF NOT EXISTS pipeline_execution_steps (
    id             VARCHAR(36)  NOT NULL,
    execution_id   VARCHAR(36)  NOT NULL,
    pipeline_id    VARCHAR(36)  NOT NULL,
    tenant_id      VARCHAR(36)  NOT NULL,
    step_order     INT          NOT NULL,
    pipelet_id     VARCHAR(64)  NOT NULL,
    status         ENUM('pending', 'running', 'completed', 'failed', 'cancelled')
                   NOT NULL DEFAULT 'pending',
    started_at     TIMESTAMP    NULL,
    completed_at   TIMESTAMP    NULL,
    records_in     BIGINT       NULL,
    records_out    BIGINT       NULL,
    error_summary  JSON         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_execution_step (execution_id, step_order),
    KEY idx_execution_steps_execution_id (execution_id),
    KEY idx_execution_steps_tenant_id (tenant_id),
    CONSTRAINT fk_execution_steps_execution
        FOREIGN KEY (execution_id) REFERENCES pipeline_executions (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_execution_steps_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
