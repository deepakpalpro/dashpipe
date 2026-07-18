-- W1-US07: seed storage connector type (S3 / LocalStack)
INSERT INTO connector_types (id, type, display_name, config_schema, spi_class, spi_version) VALUES
    (
        'ct-storage',
        'storage',
        'Object Storage (S3)',
        JSON_OBJECT(
            'type', 'object',
            'required', JSON_ARRAY('bucket', 'endpoint', 'region'),
            'properties', JSON_OBJECT(
                'bucket', JSON_OBJECT('type', 'string'),
                'endpoint', JSON_OBJECT('type', 'string'),
                'region', JSON_OBJECT('type', 'string'),
                'accessKeyId', JSON_OBJECT('type', 'string'),
                'secretAccessKey', JSON_OBJECT('type', 'string'),
                'createBucketIfMissing', JSON_OBJECT('type', 'boolean')
            )
        ),
        'com.pipelineplatform.connector.storage.StorageConnector',
        '1.0'
    );
