-- Dual config model: deployment_configuration + execution_configuration
-- Defaults live on types/pipelets; instances and pipelines can override/extend.

ALTER TABLE pipelines
    ADD COLUMN execution_config JSON NULL AFTER deployment_config;

ALTER TABLE pipeline_steps
    ADD COLUMN deployment_config JSON NULL AFTER config,
    ADD COLUMN execution_config JSON NULL AFTER deployment_config;

UPDATE pipeline_steps
SET execution_config = config
WHERE config IS NOT NULL
  AND (execution_config IS NULL OR JSON_TYPE(execution_config) = 'NULL');

ALTER TABLE connectors
    ADD COLUMN deployment_config JSON NULL AFTER config,
    ADD COLUMN execution_config JSON NULL AFTER deployment_config;

UPDATE connectors
SET execution_config = config
WHERE config IS NOT NULL
  AND (execution_config IS NULL OR JSON_TYPE(execution_config) = 'NULL');

ALTER TABLE services
    ADD COLUMN deployment_config JSON NULL AFTER tenant_config,
    ADD COLUMN execution_config JSON NULL AFTER deployment_config;

UPDATE services
SET execution_config = tenant_config
WHERE tenant_config IS NOT NULL
  AND (execution_config IS NULL OR JSON_TYPE(execution_config) = 'NULL');

ALTER TABLE service_defaults
    ADD COLUMN default_deployment_config JSON NULL AFTER default_config,
    ADD COLUMN default_execution_config JSON NULL AFTER default_deployment_config;

UPDATE service_defaults
SET default_execution_config = default_config
WHERE default_config IS NOT NULL
  AND (default_execution_config IS NULL OR JSON_TYPE(default_execution_config) = 'NULL');

UPDATE service_defaults
SET default_deployment_config = JSON_OBJECT('cloud', 'aws', 'region', 'us-east-1')
WHERE default_deployment_config IS NULL OR JSON_TYPE(default_deployment_config) = 'NULL';

ALTER TABLE connector_types
    ADD COLUMN default_deployment_config JSON NULL AFTER config_schema,
    ADD COLUMN default_execution_config JSON NULL AFTER default_deployment_config;

UPDATE connector_types
SET default_execution_config = COALESCE(config_schema, JSON_OBJECT()),
    default_deployment_config = JSON_OBJECT('cloud', 'aws', 'region', 'us-east-1')
WHERE id IS NOT NULL;
