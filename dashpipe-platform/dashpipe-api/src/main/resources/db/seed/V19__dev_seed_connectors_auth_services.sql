-- Dev seed: T001/T002, one connector per pipelet catalog entry, Auth services per vendor

INSERT INTO tenants (id, name, slug, status, credit_balance)
VALUES
  ('T001', 'Acme Analytics', 'acme-analytics', 'active', 100.0000),
  ('T002', 'Beta Logistics', 'beta-logistics', 'active', 100.0000)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  credit_balance = GREATEST(credit_balance, VALUES(credit_balance));

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-rest-source',
    'T001',
    'ct-rest',
    'REST Source (plet-rest-source)',
    '{"baseUrl":"https://demo.example.local/plet-rest-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-rest-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-webhook-source',
    'T001',
    'ct-event-listener',
    'Webhook Source (plet-webhook-source)',
    '{"path":"/hooks/plet-webhook-source","secret":"seed-webhook-secret","signature_header":"X-Hub-Signature-256"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"path":"/hooks/plet-webhook-source","secret":"seed-webhook-secret","signature_header":"X-Hub-Signature-256"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-json-transform',
    'T001',
    'ct-rest',
    'JSON Transform (plet-json-transform)',
    '{"baseUrl":"https://demo.example.local/plet-json-transform","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-json-transform","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-python-filter',
    'T001',
    'ct-rest',
    'Python Filter (plet-python-filter)',
    '{"baseUrl":"https://demo.example.local/plet-python-filter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-python-filter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-adls-destination',
    'T001',
    'ct-storage',
    'ADLS Destination (plet-adls-destination)',
    '{"bucket":"demo-adls-destination","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-adls-destination","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-s3-destination',
    'T001',
    'ct-storage',
    'S3 Destination (plet-s3-destination)',
    '{"bucket":"demo-s3-destination","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-s3-destination","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-kafka-source',
    'T001',
    'ct-message-bus',
    'Kafka Source (plet-kafka-source)',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-kafka-source","region":"us-east-1","waitTimeSeconds":10}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-kafka-source","region":"us-east-1","waitTimeSeconds":10}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-s3-source',
    'T001',
    'ct-storage',
    'S3 Source (plet-s3-source)',
    '{"bucket":"demo-s3-source","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-s3-source","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-gcs-source',
    'T001',
    'ct-storage',
    'GCS Source (plet-gcs-source)',
    '{"bucket":"demo-gcs-source","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-gcs-source","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-jdbc-source',
    'T001',
    'ct-rest',
    'JDBC Source (plet-jdbc-source)',
    '{"baseUrl":"https://demo.example.local/plet-jdbc-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-jdbc-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-mongodb-source',
    'T001',
    'ct-rest',
    'MongoDB Source (plet-mongodb-source)',
    '{"baseUrl":"https://demo.example.local/plet-mongodb-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-mongodb-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-salesforce-source',
    'T001',
    'ct-rest',
    'Salesforce Source (plet-salesforce-source)',
    '{"baseUrl":"https://demo.example.local/plet-salesforce-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-salesforce-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-file-watch-source',
    'T001',
    'ct-rest',
    'File Watch Source (plet-file-watch-source)',
    '{"baseUrl":"https://demo.example.local/plet-file-watch-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-file-watch-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-ftp-source',
    'T001',
    'ct-rest',
    'FTP Source (plet-ftp-source)',
    '{"baseUrl":"https://demo.example.local/plet-ftp-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-ftp-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-pub-sub-source',
    'T001',
    'ct-rest',
    'Pub/Sub Source (plet-pub-sub-source)',
    '{"baseUrl":"https://demo.example.local/plet-pub-sub-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-pub-sub-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-sqs-source',
    'T001',
    'ct-message-bus',
    'SQS Source (plet-sqs-source)',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-sqs-source","region":"us-east-1","waitTimeSeconds":10}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-sqs-source","region":"us-east-1","waitTimeSeconds":10}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-event-hub-source',
    'T001',
    'ct-rest',
    'Event Hub Source (plet-event-hub-source)',
    '{"baseUrl":"https://demo.example.local/plet-event-hub-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-event-hub-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-snowflake-source',
    'T001',
    'ct-rest',
    'Snowflake Source (plet-snowflake-source)',
    '{"baseUrl":"https://demo.example.local/plet-snowflake-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-snowflake-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-bigquery-source',
    'T001',
    'ct-rest',
    'BigQuery Source (plet-bigquery-source)',
    '{"baseUrl":"https://demo.example.local/plet-bigquery-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-bigquery-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-csv-source',
    'T001',
    'ct-rest',
    'CSV Source (plet-csv-source)',
    '{"baseUrl":"https://demo.example.local/plet-csv-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-csv-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-avro-source',
    'T001',
    'ct-rest',
    'Avro Source (plet-avro-source)',
    '{"baseUrl":"https://demo.example.local/plet-avro-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-avro-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-parquet-source',
    'T001',
    'ct-rest',
    'Parquet Source (plet-parquet-source)',
    '{"baseUrl":"https://demo.example.local/plet-parquet-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-parquet-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-graphql-source',
    'T001',
    'ct-rest',
    'GraphQL Source (plet-graphql-source)',
    '{"baseUrl":"https://demo.example.local/plet-graphql-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-graphql-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-imap-source',
    'T001',
    'ct-rest',
    'IMAP Source (plet-imap-source)',
    '{"baseUrl":"https://demo.example.local/plet-imap-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-imap-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-iot-hub-source',
    'T001',
    'ct-rest',
    'IoT Hub Source (plet-iot-hub-source)',
    '{"baseUrl":"https://demo.example.local/plet-iot-hub-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-iot-hub-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-cdc-source',
    'T001',
    'ct-rest',
    'CDC Source (plet-cdc-source)',
    '{"baseUrl":"https://demo.example.local/plet-cdc-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-cdc-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-schedule-source',
    'T001',
    'ct-rest',
    'Schedule Source (plet-schedule-source)',
    '{"baseUrl":"https://demo.example.local/plet-schedule-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-schedule-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-manual-source',
    'T001',
    'ct-rest',
    'Manual Source (plet-manual-source)',
    '{"baseUrl":"https://demo.example.local/plet-manual-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-manual-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-redis-stream-source',
    'T001',
    'ct-rest',
    'Redis Stream Source (plet-redis-stream-source)',
    '{"baseUrl":"https://demo.example.local/plet-redis-stream-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-redis-stream-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-nats-source',
    'T001',
    'ct-rest',
    'NATS Source (plet-nats-source)',
    '{"baseUrl":"https://demo.example.local/plet-nats-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-nats-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-amqp-source',
    'T001',
    'ct-message-bus',
    'AMQP Source (plet-amqp-source)',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-amqp-source","region":"us-east-1","waitTimeSeconds":10}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-amqp-source","region":"us-east-1","waitTimeSeconds":10}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-http-poll-source',
    'T001',
    'ct-rest',
    'HTTP Poll Source (plet-http-poll-source)',
    '{"baseUrl":"https://demo.example.local/plet-http-poll-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-http-poll-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-sharepoint-source',
    'T001',
    'ct-rest',
    'SharePoint Source (plet-sharepoint-source)',
    '{"baseUrl":"https://demo.example.local/plet-sharepoint-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-sharepoint-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-dropbox-source',
    'T001',
    'ct-rest',
    'Dropbox Source (plet-dropbox-source)',
    '{"baseUrl":"https://demo.example.local/plet-dropbox-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-dropbox-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-onedrive-source',
    'T001',
    'ct-rest',
    'OneDrive Source (plet-onedrive-source)',
    '{"baseUrl":"https://demo.example.local/plet-onedrive-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-onedrive-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-slack-source',
    'T001',
    'ct-rest',
    'Slack Source (plet-slack-source)',
    '{"baseUrl":"https://demo.example.local/plet-slack-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-slack-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-field-mapper',
    'T001',
    'ct-rest',
    'Field Mapper (plet-field-mapper)',
    '{"baseUrl":"https://demo.example.local/plet-field-mapper","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-field-mapper","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-schema-validator',
    'T001',
    'ct-rest',
    'Schema Validator (plet-schema-validator)',
    '{"baseUrl":"https://demo.example.local/plet-schema-validator","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-schema-validator","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-deduplicator',
    'T001',
    'ct-rest',
    'Deduplicator (plet-deduplicator)',
    '{"baseUrl":"https://demo.example.local/plet-deduplicator","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-deduplicator","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-aggregator',
    'T001',
    'ct-rest',
    'Aggregator (plet-aggregator)',
    '{"baseUrl":"https://demo.example.local/plet-aggregator","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-aggregator","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-joiner',
    'T001',
    'ct-rest',
    'Joiner (plet-joiner)',
    '{"baseUrl":"https://demo.example.local/plet-joiner","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-joiner","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-splitter',
    'T001',
    'ct-rest',
    'Splitter (plet-splitter)',
    '{"baseUrl":"https://demo.example.local/plet-splitter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-splitter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-enricher',
    'T001',
    'ct-rest',
    'Enricher (plet-enricher)',
    '{"baseUrl":"https://demo.example.local/plet-enricher","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-enricher","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-masker',
    'T001',
    'ct-rest',
    'Masker (plet-masker)',
    '{"baseUrl":"https://demo.example.local/plet-masker","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-masker","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-hasher',
    'T001',
    'ct-rest',
    'Hasher (plet-hasher)',
    '{"baseUrl":"https://demo.example.local/plet-hasher","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-hasher","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-rate-limiter',
    'T001',
    'ct-rest',
    'Rate Limiter (plet-rate-limiter)',
    '{"baseUrl":"https://demo.example.local/plet-rate-limiter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-rate-limiter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-retry-buffer',
    'T001',
    'ct-rest',
    'Retry Buffer (plet-retry-buffer)',
    '{"baseUrl":"https://demo.example.local/plet-retry-buffer","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-retry-buffer","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-branch-router',
    'T001',
    'ct-rest',
    'Branch Router (plet-branch-router)',
    '{"baseUrl":"https://demo.example.local/plet-branch-router","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-branch-router","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-xml-to-json',
    'T001',
    'ct-rest',
    'XML to JSON (plet-xml-to-json)',
    '{"baseUrl":"https://demo.example.local/plet-xml-to-json","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-xml-to-json","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-csv-to-json',
    'T001',
    'ct-rest',
    'CSV to JSON (plet-csv-to-json)',
    '{"baseUrl":"https://demo.example.local/plet-csv-to-json","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-csv-to-json","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-avro-codec',
    'T001',
    'ct-rest',
    'Avro Codec (plet-avro-codec)',
    '{"baseUrl":"https://demo.example.local/plet-avro-codec","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-avro-codec","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-compression',
    'T001',
    'ct-rest',
    'Compression (plet-compression)',
    '{"baseUrl":"https://demo.example.local/plet-compression","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-compression","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-encryption',
    'T001',
    'ct-rest',
    'Encryption (plet-encryption)',
    '{"baseUrl":"https://demo.example.local/plet-encryption","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-encryption","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-geo-enrich',
    'T001',
    'ct-rest',
    'Geo Enrich (plet-geo-enrich)',
    '{"baseUrl":"https://demo.example.local/plet-geo-enrich","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-geo-enrich","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-time-window',
    'T001',
    'ct-rest',
    'Time Window (plet-time-window)',
    '{"baseUrl":"https://demo.example.local/plet-time-window","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-time-window","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-sample-filter',
    'T001',
    'ct-rest',
    'Sample Filter (plet-sample-filter)',
    '{"baseUrl":"https://demo.example.local/plet-sample-filter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-sample-filter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-null-drop',
    'T001',
    'ct-rest',
    'Null Drop (plet-null-drop)',
    '{"baseUrl":"https://demo.example.local/plet-null-drop","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-null-drop","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-type-coercer',
    'T001',
    'ct-rest',
    'Type Coercer (plet-type-coercer)',
    '{"baseUrl":"https://demo.example.local/plet-type-coercer","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-type-coercer","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-regex-extract',
    'T001',
    'ct-rest',
    'Regex Extract (plet-regex-extract)',
    '{"baseUrl":"https://demo.example.local/plet-regex-extract","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-regex-extract","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-lookup-cache',
    'T001',
    'ct-rest',
    'Lookup Cache (plet-lookup-cache)',
    '{"baseUrl":"https://demo.example.local/plet-lookup-cache","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-lookup-cache","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-script-transform',
    'T001',
    'ct-rest',
    'Script Transform (plet-script-transform)',
    '{"baseUrl":"https://demo.example.local/plet-script-transform","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-script-transform","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-ml-scorer',
    'T001',
    'ct-rest',
    'ML Scorer (plet-ml-scorer)',
    '{"baseUrl":"https://demo.example.local/plet-ml-scorer","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-ml-scorer","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-anomaly-flag',
    'T001',
    'ct-rest',
    'Anomaly Flag (plet-anomaly-flag)',
    '{"baseUrl":"https://demo.example.local/plet-anomaly-flag","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-anomaly-flag","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-unit-converter',
    'T001',
    'ct-rest',
    'Unit Converter (plet-unit-converter)',
    '{"baseUrl":"https://demo.example.local/plet-unit-converter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-unit-converter","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-currency-fx',
    'T001',
    'ct-rest',
    'Currency FX (plet-currency-fx)',
    '{"baseUrl":"https://demo.example.local/plet-currency-fx","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-currency-fx","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-html-strip',
    'T001',
    'ct-rest',
    'HTML Strip (plet-html-strip)',
    '{"baseUrl":"https://demo.example.local/plet-html-strip","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-html-strip","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-language-detect',
    'T001',
    'ct-rest',
    'Language Detect (plet-language-detect)',
    '{"baseUrl":"https://demo.example.local/plet-language-detect","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-language-detect","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-sentiment-tag',
    'T001',
    'ct-rest',
    'Sentiment Tag (plet-sentiment-tag)',
    '{"baseUrl":"https://demo.example.local/plet-sentiment-tag","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-sentiment-tag","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-entity-extract',
    'T001',
    'ct-rest',
    'Entity Extract (plet-entity-extract)',
    '{"baseUrl":"https://demo.example.local/plet-entity-extract","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-entity-extract","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-json-path',
    'T001',
    'ct-rest',
    'JSON Path (plet-json-path)',
    '{"baseUrl":"https://demo.example.local/plet-json-path","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-json-path","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-flatten',
    'T001',
    'ct-rest',
    'Flatten (plet-flatten)',
    '{"baseUrl":"https://demo.example.local/plet-flatten","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-flatten","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-kafka-destination',
    'T001',
    'ct-message-bus',
    'Kafka Destination (plet-kafka-destination)',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-kafka-destination","region":"us-east-1","waitTimeSeconds":10}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-kafka-destination","region":"us-east-1","waitTimeSeconds":10}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-jdbc-destination',
    'T001',
    'ct-rest',
    'JDBC Destination (plet-jdbc-destination)',
    '{"baseUrl":"https://demo.example.local/plet-jdbc-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-jdbc-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-mongodb-destination',
    'T001',
    'ct-rest',
    'MongoDB Destination (plet-mongodb-destination)',
    '{"baseUrl":"https://demo.example.local/plet-mongodb-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-mongodb-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-elasticsearch-destination',
    'T001',
    'ct-rest',
    'Elasticsearch Destination (plet-elasticsearch-destination)',
    '{"baseUrl":"https://demo.example.local/plet-elasticsearch-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-elasticsearch-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-snowflake-destination',
    'T001',
    'ct-rest',
    'Snowflake Destination (plet-snowflake-destination)',
    '{"baseUrl":"https://demo.example.local/plet-snowflake-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-snowflake-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-bigquery-destination',
    'T001',
    'ct-rest',
    'BigQuery Destination (plet-bigquery-destination)',
    '{"baseUrl":"https://demo.example.local/plet-bigquery-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-bigquery-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-gcs-destination',
    'T001',
    'ct-storage',
    'GCS Destination (plet-gcs-destination)',
    '{"bucket":"demo-gcs-destination","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-gcs-destination","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-azure-blob-destination',
    'T001',
    'ct-storage',
    'Azure Blob Destination (plet-azure-blob-destination)',
    '{"bucket":"demo-azure-blob-destination","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-azure-blob-destination","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-webhook-destination',
    'T001',
    'ct-event-listener',
    'Webhook Destination (plet-webhook-destination)',
    '{"path":"/hooks/plet-webhook-destination","secret":"seed-webhook-secret","signature_header":"X-Hub-Signature-256"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"path":"/hooks/plet-webhook-destination","secret":"seed-webhook-secret","signature_header":"X-Hub-Signature-256"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-email-destination',
    'T001',
    'ct-rest',
    'Email Destination (plet-email-destination)',
    '{"baseUrl":"https://demo.example.local/plet-email-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-email-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-slack-destination',
    'T001',
    'ct-rest',
    'Slack Destination (plet-slack-destination)',
    '{"baseUrl":"https://demo.example.local/plet-slack-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-slack-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-teams-destination',
    'T001',
    'ct-rest',
    'Teams Destination (plet-teams-destination)',
    '{"baseUrl":"https://demo.example.local/plet-teams-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-teams-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-pub-sub-destination',
    'T001',
    'ct-rest',
    'Pub/Sub Destination (plet-pub-sub-destination)',
    '{"baseUrl":"https://demo.example.local/plet-pub-sub-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-pub-sub-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-sqs-destination',
    'T001',
    'ct-message-bus',
    'SQS Destination (plet-sqs-destination)',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-sqs-destination","region":"us-east-1","waitTimeSeconds":10}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-sqs-destination","region":"us-east-1","waitTimeSeconds":10}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-event-hub-destination',
    'T001',
    'ct-rest',
    'Event Hub Destination (plet-event-hub-destination)',
    '{"baseUrl":"https://demo.example.local/plet-event-hub-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-event-hub-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-redis-destination',
    'T001',
    'ct-rest',
    'Redis Destination (plet-redis-destination)',
    '{"baseUrl":"https://demo.example.local/plet-redis-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-redis-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-dynamodb-destination',
    'T001',
    'ct-rest',
    'DynamoDB Destination (plet-dynamodb-destination)',
    '{"baseUrl":"https://demo.example.local/plet-dynamodb-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-dynamodb-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-cosmos-db-destination',
    'T001',
    'ct-rest',
    'Cosmos DB Destination (plet-cosmos-db-destination)',
    '{"baseUrl":"https://demo.example.local/plet-cosmos-db-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-cosmos-db-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-ftp-destination',
    'T001',
    'ct-rest',
    'FTP Destination (plet-ftp-destination)',
    '{"baseUrl":"https://demo.example.local/plet-ftp-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-ftp-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-sharepoint-destination',
    'T001',
    'ct-rest',
    'SharePoint Destination (plet-sharepoint-destination)',
    '{"baseUrl":"https://demo.example.local/plet-sharepoint-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-sharepoint-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-dropbox-destination',
    'T001',
    'ct-rest',
    'Dropbox Destination (plet-dropbox-destination)',
    '{"baseUrl":"https://demo.example.local/plet-dropbox-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-dropbox-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-onedrive-destination',
    'T001',
    'ct-rest',
    'OneDrive Destination (plet-onedrive-destination)',
    '{"baseUrl":"https://demo.example.local/plet-onedrive-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-onedrive-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-salesforce-destination',
    'T001',
    'ct-rest',
    'Salesforce Destination (plet-salesforce-destination)',
    '{"baseUrl":"https://demo.example.local/plet-salesforce-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-salesforce-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-hubspot-destination',
    'T001',
    'ct-rest',
    'HubSpot Destination (plet-hubspot-destination)',
    '{"baseUrl":"https://demo.example.local/plet-hubspot-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-hubspot-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-databricks-destination',
    'T001',
    'ct-rest',
    'Databricks Destination (plet-databricks-destination)',
    '{"baseUrl":"https://demo.example.local/plet-databricks-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-databricks-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-redshift-destination',
    'T001',
    'ct-rest',
    'Redshift Destination (plet-redshift-destination)',
    '{"baseUrl":"https://demo.example.local/plet-redshift-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-redshift-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-clickhouse-destination',
    'T001',
    'ct-rest',
    'ClickHouse Destination (plet-clickhouse-destination)',
    '{"baseUrl":"https://demo.example.local/plet-clickhouse-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-clickhouse-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-prometheus-push',
    'T001',
    'ct-rest',
    'Prometheus Push (plet-prometheus-push)',
    '{"baseUrl":"https://demo.example.local/plet-prometheus-push","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-prometheus-push","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-opensearch-destination',
    'T001',
    'ct-rest',
    'OpenSearch Destination (plet-opensearch-destination)',
    '{"baseUrl":"https://demo.example.local/plet-opensearch-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-opensearch-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-file-destination',
    'T001',
    'ct-storage',
    'File Destination (plet-file-destination)',
    '{"bucket":"demo-file-destination","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-file-destination","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-null-destination',
    'T001',
    'ct-rest',
    'Null Destination (plet-null-destination)',
    '{"baseUrl":"https://demo.example.local/plet-null-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-null-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-archive-destination',
    'T001',
    'ct-rest',
    'Archive Destination (plet-archive-destination)',
    '{"baseUrl":"https://demo.example.local/plet-archive-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-archive-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-pagerduty-destination',
    'T001',
    'ct-rest',
    'PagerDuty Destination (plet-pagerduty-destination)',
    '{"baseUrl":"https://demo.example.local/plet-pagerduty-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-pagerduty-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-plet-datadog-destination',
    'T001',
    'ct-rest',
    'Datadog Destination (plet-datadog-destination)',
    '{"baseUrl":"https://demo.example.local/plet-datadog-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-datadog-destination","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-t002-plet-rest-source',
    'T002',
    'ct-rest',
    'REST Source (plet-rest-source)',
    '{"baseUrl":"https://demo.example.local/plet-rest-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"baseUrl":"https://demo.example.local/plet-rest-source","timeoutMs":30000,"api_key":"seed-demo-key"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-t002-plet-webhook-source',
    'T002',
    'ct-event-listener',
    'Webhook Source (plet-webhook-source)',
    '{"path":"/hooks/plet-webhook-source","secret":"seed-webhook-secret","signature_header":"X-Hub-Signature-256"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"path":"/hooks/plet-webhook-source","secret":"seed-webhook-secret","signature_header":"X-Hub-Signature-256"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-t002-plet-adls-destination',
    'T002',
    'ct-storage',
    'ADLS Destination (plet-adls-destination)',
    '{"bucket":"demo-adls-destination","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-adls-destination","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-t002-plet-s3-destination',
    'T002',
    'ct-storage',
    'S3 Destination (plet-s3-destination)',
    '{"bucket":"demo-s3-destination","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-s3-destination","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-t002-plet-kafka-source',
    'T002',
    'ct-message-bus',
    'Kafka Source (plet-kafka-source)',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-kafka-source","region":"us-east-1","waitTimeSeconds":10}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"queueUrl":"https://sqs.us-east-1.amazonaws.com/000000000000/plet-kafka-source","region":"us-east-1","waitTimeSeconds":10}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO connectors (
    id, tenant_id, connector_type_id, name, config, deployment_config, execution_config, status
) VALUES (
    'conn-t002-plet-s3-source',
    'T002',
    'ct-storage',
    'S3 Source (plet-s3-source)',
    '{"bucket":"demo-s3-source","region":"us-east-1","prefix":"inbound/"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"bucket":"demo-s3-source","region":"us-east-1","prefix":"inbound/"}',
    'active'
)
ON DUPLICATE KEY UPDATE
    connector_type_id = VALUES(connector_type_id),
    config = VALUES(config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    'svc-auth-oauth',
    'T001',
    'st-auth',
    'OAuth',
    'OAuth Auth',
    '{"issuer":"https://oauth.example.local","authorization_url":"https://oauth.example.local/authorize","token_url":"https://oauth.example.local/token","scopes":"openid profile","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-oauth","client_secret":"seed-oauth-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://oauth.example.local","authorization_url":"https://oauth.example.local/authorize","token_url":"https://oauth.example.local/token","scopes":"openid profile","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-oauth","client_secret":"seed-oauth-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    't002-svc-auth-oauth',
    'T002',
    'st-auth',
    'OAuth',
    'OAuth Auth (Beta)',
    '{"issuer":"https://oauth.example.local","authorization_url":"https://oauth.example.local/authorize","token_url":"https://oauth.example.local/token","scopes":"openid profile","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-oauth","client_secret":"seed-oauth-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://oauth.example.local","authorization_url":"https://oauth.example.local/authorize","token_url":"https://oauth.example.local/token","scopes":"openid profile","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-oauth","client_secret":"seed-oauth-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    'svc-auth-oidc',
    'T001',
    'st-auth',
    'OIDC',
    'OIDC Auth',
    '{"issuer":"https://oidc.example.local","discovery_url":"https://oidc.example.local/.well-known/openid-configuration","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-oidc","client_secret":"seed-oidc-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://oidc.example.local","discovery_url":"https://oidc.example.local/.well-known/openid-configuration","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-oidc","client_secret":"seed-oidc-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    't002-svc-auth-oidc',
    'T002',
    'st-auth',
    'OIDC',
    'OIDC Auth (Beta)',
    '{"issuer":"https://oidc.example.local","discovery_url":"https://oidc.example.local/.well-known/openid-configuration","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-oidc","client_secret":"seed-oidc-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://oidc.example.local","discovery_url":"https://oidc.example.local/.well-known/openid-configuration","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-oidc","client_secret":"seed-oidc-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    'svc-auth-keycloak',
    'T001',
    'st-auth',
    'Keycloak',
    'Keycloak Auth',
    '{"issuer":"https://keycloak.example.local/realms/pipeline","realm":"pipeline","auth_server_url":"https://keycloak.example.local","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-keycloak","client_secret":"seed-keycloak-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://keycloak.example.local/realms/pipeline","realm":"pipeline","auth_server_url":"https://keycloak.example.local","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-keycloak","client_secret":"seed-keycloak-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    't002-svc-auth-keycloak',
    'T002',
    'st-auth',
    'Keycloak',
    'Keycloak Auth (Beta)',
    '{"issuer":"https://keycloak.example.local/realms/pipeline","realm":"pipeline","auth_server_url":"https://keycloak.example.local","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-keycloak","client_secret":"seed-keycloak-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://keycloak.example.local/realms/pipeline","realm":"pipeline","auth_server_url":"https://keycloak.example.local","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-keycloak","client_secret":"seed-keycloak-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    'svc-auth-aad',
    'T001',
    'st-auth',
    'AAD',
    'Azure AD Auth',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"11111111-1111-1111-1111-111111111111","audience":"api://pipeline-platform","clock_skew_seconds":300,"client_id":"beta-aad-app","client_secret":"seed-aad-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"11111111-1111-1111-1111-111111111111","audience":"api://pipeline-platform","clock_skew_seconds":300,"client_id":"beta-aad-app","client_secret":"seed-aad-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    't002-svc-auth-aad',
    'T002',
    'st-auth',
    'AAD',
    'Azure AD Auth (Beta)',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"11111111-1111-1111-1111-111111111111","audience":"api://pipeline-platform","clock_skew_seconds":300,"client_id":"beta-aad-app","client_secret":"seed-aad-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"11111111-1111-1111-1111-111111111111","audience":"api://pipeline-platform","clock_skew_seconds":300,"client_id":"beta-aad-app","client_secret":"seed-aad-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    'svc-auth-cognito',
    'T001',
    'st-auth',
    'AWSCognito',
    'AWS Cognito Auth',
    '{"issuer":"https://cognito-idp.us-east-1.amazonaws.com/us-east-1_EXAMPLE","user_pool_id":"us-east-1_ACME","region":"us-east-1","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-cognito","client_secret":"seed-cognito-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://cognito-idp.us-east-1.amazonaws.com/us-east-1_EXAMPLE","user_pool_id":"us-east-1_ACME","region":"us-east-1","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-cognito","client_secret":"seed-cognito-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    't002-svc-auth-cognito',
    'T002',
    'st-auth',
    'AWSCognito',
    'AWS Cognito Auth (Beta)',
    '{"issuer":"https://cognito-idp.us-east-1.amazonaws.com/us-east-1_EXAMPLE","user_pool_id":"us-east-1_ACME","region":"us-east-1","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-cognito","client_secret":"seed-cognito-secret"}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://cognito-idp.us-east-1.amazonaws.com/us-east-1_EXAMPLE","user_pool_id":"us-east-1_ACME","region":"us-east-1","audience":"pipeline-platform","clock_skew_seconds":300,"client_id":"beta-cognito","client_secret":"seed-cognito-secret"}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    'svc-auth-azure-mi',
    'T001',
    'st-auth',
    'AzureMI',
    'Azure Managed Identity',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"11111111-1111-1111-1111-111111111111","managed_identity_client_id":"22222222-2222-2222-2222-222222222222","audience":"api://pipeline-platform","clock_skew_seconds":300}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"11111111-1111-1111-1111-111111111111","managed_identity_client_id":"22222222-2222-2222-2222-222222222222","audience":"api://pipeline-platform","clock_skew_seconds":300}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    't002-svc-auth-azure-mi',
    'T002',
    'st-auth',
    'AzureMI',
    'Azure Managed Identity (Beta)',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"11111111-1111-1111-1111-111111111111","managed_identity_client_id":"22222222-2222-2222-2222-222222222222","audience":"api://pipeline-platform","clock_skew_seconds":300}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://login.microsoftonline.com/{tenant}/v2.0","tenant_id":"11111111-1111-1111-1111-111111111111","managed_identity_client_id":"22222222-2222-2222-2222-222222222222","audience":"api://pipeline-platform","clock_skew_seconds":300}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    'svc-auth-cert',
    'T001',
    'st-auth',
    'CertBased',
    'Certificate-based Auth',
    '{"issuer":"https://certs.example.local","truststore_path":"/etc/pipeline/certs/beta-truststore.jks","certificate_subject_cn":"beta-pipeline-client","audience":"pipeline-platform","clock_skew_seconds":300}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://certs.example.local","truststore_path":"/etc/pipeline/certs/beta-truststore.jks","certificate_subject_cn":"beta-pipeline-client","audience":"pipeline-platform","clock_skew_seconds":300}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    't002-svc-auth-cert',
    'T002',
    'st-auth',
    'CertBased',
    'Certificate-based Auth (Beta)',
    '{"issuer":"https://certs.example.local","truststore_path":"/etc/pipeline/certs/beta-truststore.jks","certificate_subject_cn":"beta-pipeline-client","audience":"pipeline-platform","clock_skew_seconds":300}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://certs.example.local","truststore_path":"/etc/pipeline/certs/beta-truststore.jks","certificate_subject_cn":"beta-pipeline-client","audience":"pipeline-platform","clock_skew_seconds":300}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    'svc-auth-jwt',
    'T001',
    'st-auth',
    'JWT',
    'JWT Auth',
    '{"issuer":"https://jwt.example.local","jwks_url":"https://jwt.beta.example.local/.well-known/jwks.json","audience":"pipeline-platform","algorithm":"RS256","clock_skew_seconds":300}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://jwt.example.local","jwks_url":"https://jwt.beta.example.local/.well-known/jwks.json","audience":"pipeline-platform","algorithm":"RS256","clock_skew_seconds":300}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);

INSERT INTO services (
    id, tenant_id, service_type_id, vendor, name, tenant_config,
    deployment_config, execution_config, inherits_default, status
) VALUES (
    't002-svc-auth-jwt',
    'T002',
    'st-auth',
    'JWT',
    'JWT Auth (Beta)',
    '{"issuer":"https://jwt.example.local","jwks_url":"https://jwt.beta.example.local/.well-known/jwks.json","audience":"pipeline-platform","algorithm":"RS256","clock_skew_seconds":300}',
    '{"cloud":"aws","region":"us-east-1"}',
    '{"issuer":"https://jwt.example.local","jwks_url":"https://jwt.beta.example.local/.well-known/jwks.json","audience":"pipeline-platform","algorithm":"RS256","clock_skew_seconds":300}',
    TRUE,
    'active'
)
ON DUPLICATE KEY UPDATE
    vendor = VALUES(vendor),
    tenant_config = VALUES(tenant_config),
    deployment_config = VALUES(deployment_config),
    execution_config = VALUES(execution_config),
    inherits_default = VALUES(inherits_default),
    status = VALUES(status);
