-- W2-US02: pipeline step sequence (pipelet registry FK deferred; store pipelet_id as opaque id)
CREATE TABLE pipeline_steps (
    id                VARCHAR(36)  NOT NULL,
    pipeline_id       VARCHAR(36)  NOT NULL,
    pipelet_id        VARCHAR(36)  NOT NULL,
    step_order        INT          NOT NULL,
    config            JSON         NULL,
    connector_ids     JSON         NULL,
    service_ids       JSON         NULL,
    input_queue       VARCHAR(255) NULL,
    output_queue      VARCHAR(255) NULL,
    resource_limits   JSON         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pipeline_steps_order (pipeline_id, step_order),
    KEY idx_pipeline_steps_pipeline_id (pipeline_id),
    CONSTRAINT fk_pipeline_steps_pipeline
        FOREIGN KEY (pipeline_id) REFERENCES pipelines (id) ON DELETE CASCADE
);
