-- Fix CSV Source connector: use storage (object store) instead of placeholder REST.
UPDATE connectors
SET
    connector_type_id = 'ct-storage',
    name = 'CSV Source (plet-csv-source)',
    config = JSON_OBJECT(
        'bucket', 'demo-csv-source',
        'region', 'us-east-1',
        'endpoint', 'http://host.docker.internal:4567',
        'accessKeyId', 'test',
        'secretAccessKey', 'test'
    ),
    deployment_config = JSON_OBJECT('cloud', 'aws', 'region', 'us-east-1', 'endpoint', 'http://host.docker.internal:4567'),
    execution_config = JSON_OBJECT(
        'bucket', 'demo-csv-source',
        'region', 'us-east-1',
        'endpoint', 'http://host.docker.internal:4567',
        'accessKeyId', 'test',
        'secretAccessKey', 'test'
    ),
    status = 'active'
WHERE id = 'conn-plet-csv-source'
  AND tenant_id = 'T001';

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-csv-source',
    'T001',
    'ct-storage',
    'CSV Source (plet-csv-source)',
    '{"bucket":"demo-csv-source","region":"us-east-1","endpoint":"http://host.docker.internal:4567","accessKeyId":"test","secretAccessKey":"test"}',
    '{"cloud":"aws","region":"us-east-1","endpoint":"http://host.docker.internal:4567"}',
    '{"bucket":"demo-csv-source","region":"us-east-1","endpoint":"http://host.docker.internal:4567","accessKeyId":"test","secretAccessKey":"test"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    name = VALUES(name),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);
