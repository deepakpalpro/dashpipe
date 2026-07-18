-- Pipeline cloud deployment properties (cloud, region, accessKey, etc.)
ALTER TABLE pipelines
    ADD COLUMN deployment_config JSON NULL AFTER retry_config;
