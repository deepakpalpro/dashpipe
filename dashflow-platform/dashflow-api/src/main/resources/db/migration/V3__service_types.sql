-- W1-US03: platform service type catalog + defaults (Auth stub for US04/W3)
CREATE TABLE service_types (
    id            VARCHAR(36)  NOT NULL,
    type          ENUM('auth', 'notification', 'logging') NOT NULL,
    display_name  VARCHAR(128) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_service_types_type (type)
);

CREATE TABLE service_defaults (
    id                  VARCHAR(36)  NOT NULL,
    service_type_id     VARCHAR(36)  NOT NULL,
    vendor              VARCHAR(64)  NOT NULL,
    base_service_class  VARCHAR(512) NULL,
    default_config      JSON         NOT NULL,
    config_schema       JSON         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_service_defaults_type_vendor (service_type_id, vendor),
    CONSTRAINT fk_service_defaults_type
        FOREIGN KEY (service_type_id) REFERENCES service_types (id)
);

INSERT INTO service_types (id, type, display_name) VALUES
    ('st-auth', 'auth', 'Authentication');

INSERT INTO service_defaults (id, service_type_id, vendor, base_service_class, default_config, config_schema) VALUES
    (
        'sd-auth-stub',
        'st-auth',
        'StubAuth',
        'com.pipelineplatform.api.service.StubAuthService',
        JSON_OBJECT(
            'issuer', 'https://auth.example.local/stub',
            'audience', 'pipeline-platform',
            'signature_header', 'X-Hub-Signature-256',
            'clock_skew_seconds', 300
        ),
        JSON_OBJECT(
            'type', 'object',
            'required', JSON_ARRAY('issuer'),
            'properties', JSON_OBJECT(
                'issuer', JSON_OBJECT('type', 'string'),
                'audience', JSON_OBJECT('type', 'string'),
                'client_id', JSON_OBJECT('type', 'string'),
                'client_secret', JSON_OBJECT('type', 'string', 'writeOnly', true),
                'signature_header', JSON_OBJECT('type', 'string'),
                'clock_skew_seconds', JSON_OBJECT('type', 'integer')
            )
        )
    );
