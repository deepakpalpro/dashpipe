-- W1-US08: seed message_bus connector type (SQS / LocalStack)
INSERT INTO connector_types (id, type, display_name, config_schema, spi_class, spi_version) VALUES
    (
        'ct-message-bus',
        'message_bus',
        'Message Bus (SQS)',
        JSON_OBJECT(
            'type', 'object',
            'required', JSON_ARRAY('endpoint', 'region'),
            'properties', JSON_OBJECT(
                'queueName', JSON_OBJECT('type', 'string'),
                'queueUrl', JSON_OBJECT('type', 'string'),
                'endpoint', JSON_OBJECT('type', 'string'),
                'region', JSON_OBJECT('type', 'string'),
                'createQueueIfMissing', JSON_OBJECT('type', 'boolean'),
                'accessKeyId', JSON_OBJECT('type', 'string'),
                'secretAccessKey', JSON_OBJECT('type', 'string')
            )
        ),
        'com.pipelineplatform.connector.messagebus.MessageBusConnector',
        '1.0'
    );
