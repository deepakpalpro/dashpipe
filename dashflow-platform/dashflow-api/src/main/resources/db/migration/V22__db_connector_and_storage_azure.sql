-- Seed db connector type + extend storage schema for Azure Blob / ADLS fields.
INSERT INTO connector_types (id, type, display_name, config_schema, spi_class, spi_version) VALUES
    (
        'ct-db',
        'db',
        'Database (JDBC)',
        JSON_OBJECT(
            'type', 'object',
            'required', JSON_ARRAY('jdbcUrl'),
            'properties', JSON_OBJECT(
                'jdbcUrl', JSON_OBJECT('type', 'string'),
                'username', JSON_OBJECT('type', 'string'),
                'password', JSON_OBJECT('type', 'string'),
                'driver', JSON_OBJECT('type', 'string'),
                'database', JSON_OBJECT('type', 'string'),
                'schema', JSON_OBJECT('type', 'string'),
                'warehouse', JSON_OBJECT('type', 'string'),
                'projectId', JSON_OBJECT('type', 'string')
            )
        ),
        'com.dashflow.connector.db.DbConnector',
        '1.0'
    );

UPDATE connector_types
SET config_schema = JSON_OBJECT(
    'type', 'object',
    'required', JSON_ARRAY('bucket'),
    'properties', JSON_OBJECT(
        'bucket', JSON_OBJECT('type', 'string', 'description', 'S3 bucket or Azure container alias'),
        'endpoint', JSON_OBJECT('type', 'string'),
        'region', JSON_OBJECT('type', 'string'),
        'accessKeyId', JSON_OBJECT('type', 'string'),
        'secretAccessKey', JSON_OBJECT('type', 'string'),
        'createBucketIfMissing', JSON_OBJECT('type', 'boolean'),
        'account', JSON_OBJECT('type', 'string', 'description', 'Azure storage account name'),
        'container', JSON_OBJECT('type', 'string', 'description', 'Azure Blob / ADLS container'),
        'provider', JSON_OBJECT('type', 'string', 'description', 's3 | azure_blob | adls | gcs')
    )
)
WHERE id = 'ct-storage';
