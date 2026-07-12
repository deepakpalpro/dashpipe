-- W3-US01: Event Listener connector type for webhook ingress
INSERT INTO connector_types (id, type, display_name, config_schema, spi_class, spi_version)
VALUES (
    'ct-event-listener',
    'event_listener',
    'Event Listener / Webhook',
    JSON_OBJECT(
        'type', 'object',
        'properties', JSON_OBJECT(
            'signing_secret', JSON_OBJECT('type', 'string'),
            'path_hint', JSON_OBJECT('type', 'string')
        )
    ),
    'com.pipelineplatform.connector.spi.Connector',
    '1.0'
)
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);
