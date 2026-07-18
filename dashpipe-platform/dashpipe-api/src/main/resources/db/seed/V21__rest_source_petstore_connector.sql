-- Point REST Source seed connector at petstore-inventory mock (GET catalog on :4011).
UPDATE connectors
SET
    connector_type_id = 'ct-rest',
    name = 'REST Source (plet-rest-source)',
    config = JSON_OBJECT(
        'baseUrl', 'http://host.docker.internal:4011/api/v3',
        'timeoutMs', 30000,
        'pingPath', '/inventory/summary'
    ),
    deployment_config = JSON_OBJECT('cloud', 'local', 'region', 'us-east-1'),
    execution_config = JSON_OBJECT(
        'baseUrl', 'http://host.docker.internal:4011/api/v3',
        'timeoutMs', 30000,
        'pingPath', '/inventory/summary'
    ),
    status = 'active'
WHERE id = 'conn-plet-rest-source'
  AND tenant_id = 'T001';

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-rest-source',
    'T001',
    'ct-rest',
    'REST Source (plet-rest-source)',
    '{"baseUrl":"http://host.docker.internal:4011/api/v3","timeoutMs":30000,"pingPath":"/inventory/summary"}',
    '{"cloud":"local","region":"us-east-1"}',
    '{"baseUrl":"http://host.docker.internal:4011/api/v3","timeoutMs":30000,"pingPath":"/inventory/summary"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    name = VALUES(name),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);
