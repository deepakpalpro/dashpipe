-- W1-US05: connector SPI type catalog (Rest plugin first)
CREATE TABLE connector_types (
    id             VARCHAR(36)  NOT NULL,
    type           ENUM('rest', 'grpc', 'event_listener', 'message_bus', 'db', 'storage') NOT NULL,
    display_name   VARCHAR(128) NOT NULL,
    config_schema  JSON         NULL,
    spi_class      VARCHAR(512) NOT NULL,
    spi_version    VARCHAR(16)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_connector_types_type (type)
);

INSERT INTO connector_types (id, type, display_name, config_schema, spi_class, spi_version) VALUES
    (
        'ct-rest',
        'rest',
        'REST / HTTP',
        JSON_OBJECT(
            'type', 'object',
            'required', JSON_ARRAY('baseUrl'),
            'properties', JSON_OBJECT(
                'baseUrl', JSON_OBJECT('type', 'string'),
                'timeoutMs', JSON_OBJECT('type', 'integer')
            )
        ),
        'com.pipelineplatform.connector.rest.RestConnector',
        '1.0'
    );
