-- Auth vendor catalog under st-auth (OAuth, OIDC, Keycloak, AAD, Cognito, Azure MI, Cert, JWT)
-- Idempotent: safe to re-apply via ON DUPLICATE KEY UPDATE

INSERT INTO service_defaults (
    id, service_type_id, vendor, base_service_class,
    default_config, config_schema, default_deployment_config, default_execution_config
) VALUES (
    'sd-auth-oauth',
    'st-auth',
    'OAuth',
    NULL,
    '{"issuer":"https://oauth.example.local","authorization_url":"https://oauth.example.local/authorize","token_url":"https://oauth.example.local/token","scopes":"openid profile","audience":"pipeline-platform","clock_skew_seconds":300}',
    '{"type":"object","required":["issuer","client_id","client_secret","authorization_url","token_url"],"properties":{"issuer":{"type":"string"},"audience":{"type":"string"},"client_id":{"type":"string"},"client_secret":{"type":"string","writeOnly":true},"clock_skew_seconds":{"type":"integer"},"authorization_url":{"type":"string"},"token_url":{"type":"string"},"scopes":{"type":"string"}}}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://oauth.example.local","authorization_url":"https://oauth.example.local/authorize","token_url":"https://oauth.example.local/token","scopes":"openid profile","audience":"pipeline-platform","clock_skew_seconds":300}'
)
ON DUPLICATE KEY UPDATE
    default_config = VALUES(default_config),
    config_schema = VALUES(config_schema),
    default_deployment_config = VALUES(default_deployment_config),
    default_execution_config = VALUES(default_execution_config);

INSERT INTO service_defaults (
    id, service_type_id, vendor, base_service_class,
    default_config, config_schema, default_deployment_config, default_execution_config
) VALUES (
    'sd-auth-oidc',
    'st-auth',
    'OIDC',
    NULL,
    '{"issuer":"https://oidc.example.local","discovery_url":"https://oidc.example.local/.well-known/openid-configuration","audience":"pipeline-platform","clock_skew_seconds":300}',
    '{"type":"object","required":["issuer","client_id","client_secret","discovery_url"],"properties":{"issuer":{"type":"string"},"audience":{"type":"string"},"client_id":{"type":"string"},"client_secret":{"type":"string","writeOnly":true},"clock_skew_seconds":{"type":"integer"},"discovery_url":{"type":"string"}}}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://oidc.example.local","discovery_url":"https://oidc.example.local/.well-known/openid-configuration","audience":"pipeline-platform","clock_skew_seconds":300}'
)
ON DUPLICATE KEY UPDATE
    default_config = VALUES(default_config),
    config_schema = VALUES(config_schema),
    default_deployment_config = VALUES(default_deployment_config),
    default_execution_config = VALUES(default_execution_config);

INSERT INTO service_defaults (
    id, service_type_id, vendor, base_service_class,
    default_config, config_schema, default_deployment_config, default_execution_config
) VALUES (
    'sd-auth-keycloak',
    'st-auth',
    'Keycloak',
    NULL,
    '{"issuer":"https://keycloak.example.local/realms/pipeline","realm":"pipeline","auth_server_url":"https://keycloak.example.local","audience":"pipeline-platform","clock_skew_seconds":300}',
    '{"type":"object","required":["issuer","realm","client_id","client_secret"],"properties":{"issuer":{"type":"string"},"audience":{"type":"string"},"client_id":{"type":"string"},"client_secret":{"type":"string","writeOnly":true},"clock_skew_seconds":{"type":"integer"},"realm":{"type":"string"},"auth_server_url":{"type":"string"}}}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://keycloak.example.local/realms/pipeline","realm":"pipeline","auth_server_url":"https://keycloak.example.local","audience":"pipeline-platform","clock_skew_seconds":300}'
)
ON DUPLICATE KEY UPDATE
    default_config = VALUES(default_config),
    config_schema = VALUES(config_schema),
    default_deployment_config = VALUES(default_deployment_config),
    default_execution_config = VALUES(default_execution_config);

INSERT INTO service_defaults (
    id, service_type_id, vendor, base_service_class,
    default_config, config_schema, default_deployment_config, default_execution_config
) VALUES (
    'sd-auth-aad',
    'st-auth',
    'AAD',
    NULL,
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"00000000-0000-0000-0000-000000000000","audience":"api://pipeline-platform","clock_skew_seconds":300}',
    '{"type":"object","required":["issuer","tenant_id","client_id","client_secret"],"properties":{"issuer":{"type":"string"},"audience":{"type":"string"},"client_id":{"type":"string"},"client_secret":{"type":"string","writeOnly":true},"clock_skew_seconds":{"type":"integer"},"tenant_id":{"type":"string"}}}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"00000000-0000-0000-0000-000000000000","audience":"api://pipeline-platform","clock_skew_seconds":300}'
)
ON DUPLICATE KEY UPDATE
    default_config = VALUES(default_config),
    config_schema = VALUES(config_schema),
    default_deployment_config = VALUES(default_deployment_config),
    default_execution_config = VALUES(default_execution_config);

INSERT INTO service_defaults (
    id, service_type_id, vendor, base_service_class,
    default_config, config_schema, default_deployment_config, default_execution_config
) VALUES (
    'sd-auth-cognito',
    'st-auth',
    'AWSCognito',
    NULL,
    '{"issuer":"https://cognito-idp.us-east-1.amazonaws.com/us-east-1_EXAMPLE","user_pool_id":"us-east-1_EXAMPLE","region":"us-east-1","audience":"pipeline-platform","clock_skew_seconds":300}',
    '{"type":"object","required":["issuer","user_pool_id","client_id","client_secret","region"],"properties":{"issuer":{"type":"string"},"audience":{"type":"string"},"client_id":{"type":"string"},"client_secret":{"type":"string","writeOnly":true},"clock_skew_seconds":{"type":"integer"},"user_pool_id":{"type":"string"},"region":{"type":"string"}}}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://cognito-idp.us-east-1.amazonaws.com/us-east-1_EXAMPLE","user_pool_id":"us-east-1_EXAMPLE","region":"us-east-1","audience":"pipeline-platform","clock_skew_seconds":300}'
)
ON DUPLICATE KEY UPDATE
    default_config = VALUES(default_config),
    config_schema = VALUES(config_schema),
    default_deployment_config = VALUES(default_deployment_config),
    default_execution_config = VALUES(default_execution_config);

INSERT INTO service_defaults (
    id, service_type_id, vendor, base_service_class,
    default_config, config_schema, default_deployment_config, default_execution_config
) VALUES (
    'sd-auth-azure-mi',
    'st-auth',
    'AzureMI',
    NULL,
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"00000000-0000-0000-0000-000000000000","managed_identity_client_id":"","audience":"api://pipeline-platform","clock_skew_seconds":300}',
    '{"type":"object","required":["issuer","tenant_id","audience"],"properties":{"issuer":{"type":"string"},"audience":{"type":"string"},"client_id":{"type":"string"},"client_secret":{"type":"string","writeOnly":true},"clock_skew_seconds":{"type":"integer"},"tenant_id":{"type":"string"},"managed_identity_client_id":{"type":"string"}}}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"00000000-0000-0000-0000-000000000000","managed_identity_client_id":"","audience":"api://pipeline-platform","clock_skew_seconds":300}'
)
ON DUPLICATE KEY UPDATE
    default_config = VALUES(default_config),
    config_schema = VALUES(config_schema),
    default_deployment_config = VALUES(default_deployment_config),
    default_execution_config = VALUES(default_execution_config);

INSERT INTO service_defaults (
    id, service_type_id, vendor, base_service_class,
    default_config, config_schema, default_deployment_config, default_execution_config
) VALUES (
    'sd-auth-cert',
    'st-auth',
    'CertBased',
    NULL,
    '{"issuer":"https://certs.example.local","truststore_path":"/etc/pipeline/certs/truststore.jks","certificate_subject_cn":"pipeline-client","audience":"pipeline-platform","clock_skew_seconds":300}',
    '{"type":"object","required":["issuer","truststore_path"],"properties":{"issuer":{"type":"string"},"audience":{"type":"string"},"client_id":{"type":"string"},"client_secret":{"type":"string","writeOnly":true},"clock_skew_seconds":{"type":"integer"},"truststore_path":{"type":"string"},"certificate_subject_cn":{"type":"string"}}}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://certs.example.local","truststore_path":"/etc/pipeline/certs/truststore.jks","certificate_subject_cn":"pipeline-client","audience":"pipeline-platform","clock_skew_seconds":300}'
)
ON DUPLICATE KEY UPDATE
    default_config = VALUES(default_config),
    config_schema = VALUES(config_schema),
    default_deployment_config = VALUES(default_deployment_config),
    default_execution_config = VALUES(default_execution_config);

INSERT INTO service_defaults (
    id, service_type_id, vendor, base_service_class,
    default_config, config_schema, default_deployment_config, default_execution_config
) VALUES (
    'sd-auth-jwt',
    'st-auth',
    'JWT',
    NULL,
    '{"issuer":"https://jwt.example.local","jwks_url":"https://jwt.example.local/.well-known/jwks.json","audience":"pipeline-platform","algorithm":"RS256","clock_skew_seconds":300}',
    '{"type":"object","required":["issuer","jwks_url","audience"],"properties":{"issuer":{"type":"string"},"audience":{"type":"string"},"client_id":{"type":"string"},"client_secret":{"type":"string","writeOnly":true},"clock_skew_seconds":{"type":"integer"},"jwks_url":{"type":"string"},"algorithm":{"type":"string"}}}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://jwt.example.local","jwks_url":"https://jwt.example.local/.well-known/jwks.json","audience":"pipeline-platform","algorithm":"RS256","clock_skew_seconds":300}'
)
ON DUPLICATE KEY UPDATE
    default_config = VALUES(default_config),
    config_schema = VALUES(config_schema),
    default_deployment_config = VALUES(default_deployment_config),
    default_execution_config = VALUES(default_execution_config);
