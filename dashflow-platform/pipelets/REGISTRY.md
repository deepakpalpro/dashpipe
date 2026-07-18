# Pipelet runtime registry


Catalog pipelets live under `pipelets/{source|transformer|destination}/<group>/<id>/`.
Image tags stay flat (`dashflow/<id>:local`). Path index: [`PATHS.json`](PATHS.json).

```bash
python3 scripts/generate_catalog_pipelets.py
python3 scripts/reorganize_pipelets.py   # only if moving flat dirs again
```

## Layout

| Top | Groups |
|-----|--------|
| `source/` | `database`, `http`, `messaging`, `storage`, `trigger` |
| `transformer/` | `enrich`, `extract`, `filter`, `quality`, `security`, `structure`, `transform` |
| `destination/` | `database`, `messaging`, `notify`, `saas`, `storage`, `util` |

## Core (Wave 0)

| Pipelet ID | Path | Image |
|------------|------|-------|
| `plet-s3-source` | `source/storage/plet-s3-source` | `dashflow/plet-s3-source:local` |
| `plet-csv-source` | `source/storage/plet-csv-source` | `dashflow/plet-csv-source:local` |
| `plet-rest-source` | `source/http/plet-rest-source` | `dashflow/plet-rest-source:local` |
| `plet-csv-to-json` | `transformer/transform/plet-csv-to-json` | `dashflow/plet-csv-to-json:local` |
| `plet-python-filter` | `transformer/filter/plet-python-filter` | `dashflow/plet-python-filter:local` |
| `plet-field-mapper` | `transformer/transform/plet-field-mapper` | `dashflow/plet-field-mapper:local` |
| `plet-webhook-destination` | `destination/notify/plet-webhook-destination` | `dashflow/plet-webhook-destination:local` |
| `inventory-batch` | `inventory/` | `dashflow/inventory-pipelet:local` |

## Data plane (I/O transport)

Pipeline `execution_config.ioMode` controls how stage data moves. The orchestrator injects:

| Env | Purpose |
|-----|---------|
| `IO_MODE` | `stdio` \| `queue` (default **`queue`** when unset) |
| `INPUT_QUEUE` | RabbitMQ stage input queue name |
| `OUTPUT_QUEUE` | Next stage input queue (empty on last stage) |
| `AMQP_URL` | e.g. `amqp://pipeline:pipeline@rabbitmq:5672/` |
| `SOURCE_TRIGGER` | For sources in queue mode: `once` skips waiting for a kickoff message |

- **`stdio`** — read one JSON object from stdin; write one JSON object to stdout.
- **`queue`** — consume from `INPUT_QUEUE`, publish to `OUTPUT_QUEUE` via AMQP.

Shared helper: [`_common/io_transport.py`](_common/io_transport.py).

```bash
# Build context is always pipelets/ (needs _common)
id=plet-rest-source
rel=$(python3 -c "import json; print(json.load(open('pipelets/PATHS.json'))['$id'])")
docker build -f "pipelets/$rel/Dockerfile" -t "dashflow/${id}:local" pipelets
```

## All catalog images

| ID | Path | Image |
|----|------|-------|
| `plet-adls-destination` | `destination/storage/plet-adls-destination` | `dashflow/plet-adls-destination:local` |
| `plet-aggregator` | `transformer/structure/plet-aggregator` | `dashflow/plet-aggregator:local` |
| `plet-amqp-source` | `source/messaging/plet-amqp-source` | `dashflow/plet-amqp-source:local` |
| `plet-anomaly-flag` | `transformer/quality/plet-anomaly-flag` | `dashflow/plet-anomaly-flag:local` |
| `plet-archive-destination` | `destination/storage/plet-archive-destination` | `dashflow/plet-archive-destination:local` |
| `plet-avro-codec` | `transformer/transform/plet-avro-codec` | `dashflow/plet-avro-codec:local` |
| `plet-avro-source` | `source/storage/plet-avro-source` | `dashflow/plet-avro-source:local` |
| `plet-azure-blob-destination` | `destination/storage/plet-azure-blob-destination` | `dashflow/plet-azure-blob-destination:local` |
| `plet-bigquery-destination` | `destination/database/plet-bigquery-destination` | `dashflow/plet-bigquery-destination:local` |
| `plet-bigquery-source` | `source/database/plet-bigquery-source` | `dashflow/plet-bigquery-source:local` |
| `plet-branch-router` | `transformer/structure/plet-branch-router` | `dashflow/plet-branch-router:local` |
| `plet-cdc-source` | `source/database/plet-cdc-source` | `dashflow/plet-cdc-source:local` |
| `plet-clickhouse-destination` | `destination/database/plet-clickhouse-destination` | `dashflow/plet-clickhouse-destination:local` |
| `plet-compression` | `transformer/security/plet-compression` | `dashflow/plet-compression:local` |
| `plet-cosmos-db-destination` | `destination/database/plet-cosmos-db-destination` | `dashflow/plet-cosmos-db-destination:local` |
| `plet-csv-source` | `source/storage/plet-csv-source` | `dashflow/plet-csv-source:local` |
| `plet-csv-to-json` | `transformer/transform/plet-csv-to-json` | `dashflow/plet-csv-to-json:local` |
| `plet-currency-fx` | `transformer/enrich/plet-currency-fx` | `dashflow/plet-currency-fx:local` |
| `plet-databricks-destination` | `destination/database/plet-databricks-destination` | `dashflow/plet-databricks-destination:local` |
| `plet-datadog-destination` | `destination/notify/plet-datadog-destination` | `dashflow/plet-datadog-destination:local` |
| `plet-deduplicator` | `transformer/filter/plet-deduplicator` | `dashflow/plet-deduplicator:local` |
| `plet-dropbox-destination` | `destination/saas/plet-dropbox-destination` | `dashflow/plet-dropbox-destination:local` |
| `plet-dropbox-source` | `source/http/plet-dropbox-source` | `dashflow/plet-dropbox-source:local` |
| `plet-dynamodb-destination` | `destination/database/plet-dynamodb-destination` | `dashflow/plet-dynamodb-destination:local` |
| `plet-elasticsearch-destination` | `destination/database/plet-elasticsearch-destination` | `dashflow/plet-elasticsearch-destination:local` |
| `plet-email-destination` | `destination/notify/plet-email-destination` | `dashflow/plet-email-destination:local` |
| `plet-encryption` | `transformer/security/plet-encryption` | `dashflow/plet-encryption:local` |
| `plet-enricher` | `transformer/enrich/plet-enricher` | `dashflow/plet-enricher:local` |
| `plet-entity-extract` | `transformer/extract/plet-entity-extract` | `dashflow/plet-entity-extract:local` |
| `plet-event-hub-destination` | `destination/messaging/plet-event-hub-destination` | `dashflow/plet-event-hub-destination:local` |
| `plet-event-hub-source` | `source/messaging/plet-event-hub-source` | `dashflow/plet-event-hub-source:local` |
| `plet-field-mapper` | `transformer/transform/plet-field-mapper` | `dashflow/plet-field-mapper:local` |
| `plet-file-destination` | `destination/storage/plet-file-destination` | `dashflow/plet-file-destination:local` |
| `plet-file-watch-source` | `source/storage/plet-file-watch-source` | `dashflow/plet-file-watch-source:local` |
| `plet-flatten` | `transformer/transform/plet-flatten` | `dashflow/plet-flatten:local` |
| `plet-ftp-destination` | `destination/storage/plet-ftp-destination` | `dashflow/plet-ftp-destination:local` |
| `plet-ftp-source` | `source/storage/plet-ftp-source` | `dashflow/plet-ftp-source:local` |
| `plet-gcs-destination` | `destination/storage/plet-gcs-destination` | `dashflow/plet-gcs-destination:local` |
| `plet-gcs-source` | `source/storage/plet-gcs-source` | `dashflow/plet-gcs-source:local` |
| `plet-geo-enrich` | `transformer/enrich/plet-geo-enrich` | `dashflow/plet-geo-enrich:local` |
| `plet-graphql-source` | `source/http/plet-graphql-source` | `dashflow/plet-graphql-source:local` |
| `plet-hasher` | `transformer/security/plet-hasher` | `dashflow/plet-hasher:local` |
| `plet-html-strip` | `transformer/extract/plet-html-strip` | `dashflow/plet-html-strip:local` |
| `plet-http-poll-source` | `source/http/plet-http-poll-source` | `dashflow/plet-http-poll-source:local` |
| `plet-hubspot-destination` | `destination/saas/plet-hubspot-destination` | `dashflow/plet-hubspot-destination:local` |
| `plet-imap-source` | `source/http/plet-imap-source` | `dashflow/plet-imap-source:local` |
| `plet-iot-hub-source` | `source/messaging/plet-iot-hub-source` | `dashflow/plet-iot-hub-source:local` |
| `plet-jdbc-destination` | `destination/database/plet-jdbc-destination` | `dashflow/plet-jdbc-destination:local` |
| `plet-jdbc-source` | `source/database/plet-jdbc-source` | `dashflow/plet-jdbc-source:local` |
| `plet-joiner` | `transformer/structure/plet-joiner` | `dashflow/plet-joiner:local` |
| `plet-json-path` | `transformer/transform/plet-json-path` | `dashflow/plet-json-path:local` |
| `plet-json-transform` | `transformer/transform/plet-json-transform` | `dashflow/plet-json-transform:local` |
| `plet-kafka-destination` | `destination/messaging/plet-kafka-destination` | `dashflow/plet-kafka-destination:local` |
| `plet-kafka-source` | `source/messaging/plet-kafka-source` | `dashflow/plet-kafka-source:local` |
| `plet-language-detect` | `transformer/extract/plet-language-detect` | `dashflow/plet-language-detect:local` |
| `plet-lookup-cache` | `transformer/enrich/plet-lookup-cache` | `dashflow/plet-lookup-cache:local` |
| `plet-manual-source` | `source/trigger/plet-manual-source` | `dashflow/plet-manual-source:local` |
| `plet-masker` | `transformer/security/plet-masker` | `dashflow/plet-masker:local` |
| `plet-ml-scorer` | `transformer/quality/plet-ml-scorer` | `dashflow/plet-ml-scorer:local` |
| `plet-mongodb-destination` | `destination/database/plet-mongodb-destination` | `dashflow/plet-mongodb-destination:local` |
| `plet-mongodb-source` | `source/database/plet-mongodb-source` | `dashflow/plet-mongodb-source:local` |
| `plet-nats-source` | `source/messaging/plet-nats-source` | `dashflow/plet-nats-source:local` |
| `plet-null-destination` | `destination/util/plet-null-destination` | `dashflow/plet-null-destination:local` |
| `plet-null-drop` | `transformer/filter/plet-null-drop` | `dashflow/plet-null-drop:local` |
| `plet-onedrive-destination` | `destination/saas/plet-onedrive-destination` | `dashflow/plet-onedrive-destination:local` |
| `plet-onedrive-source` | `source/http/plet-onedrive-source` | `dashflow/plet-onedrive-source:local` |
| `plet-opensearch-destination` | `destination/database/plet-opensearch-destination` | `dashflow/plet-opensearch-destination:local` |
| `plet-pagerduty-destination` | `destination/notify/plet-pagerduty-destination` | `dashflow/plet-pagerduty-destination:local` |
| `plet-parquet-source` | `source/storage/plet-parquet-source` | `dashflow/plet-parquet-source:local` |
| `plet-prometheus-push` | `destination/notify/plet-prometheus-push` | `dashflow/plet-prometheus-push:local` |
| `plet-pub-sub-destination` | `destination/messaging/plet-pub-sub-destination` | `dashflow/plet-pub-sub-destination:local` |
| `plet-pub-sub-source` | `source/messaging/plet-pub-sub-source` | `dashflow/plet-pub-sub-source:local` |
| `plet-python-filter` | `transformer/filter/plet-python-filter` | `dashflow/plet-python-filter:local` |
| `plet-rate-limiter` | `transformer/quality/plet-rate-limiter` | `dashflow/plet-rate-limiter:local` |
| `plet-redis-destination` | `destination/database/plet-redis-destination` | `dashflow/plet-redis-destination:local` |
| `plet-redis-stream-source` | `source/messaging/plet-redis-stream-source` | `dashflow/plet-redis-stream-source:local` |
| `plet-redshift-destination` | `destination/database/plet-redshift-destination` | `dashflow/plet-redshift-destination:local` |
| `plet-regex-extract` | `transformer/extract/plet-regex-extract` | `dashflow/plet-regex-extract:local` |
| `plet-rest-source` | `source/http/plet-rest-source` | `dashflow/plet-rest-source:local` |
| `plet-retry-buffer` | `transformer/quality/plet-retry-buffer` | `dashflow/plet-retry-buffer:local` |
| `plet-s3-destination` | `destination/storage/plet-s3-destination` | `dashflow/plet-s3-destination:local` |
| `plet-s3-source` | `source/storage/plet-s3-source` | `dashflow/plet-s3-source:local` |
| `plet-salesforce-destination` | `destination/saas/plet-salesforce-destination` | `dashflow/plet-salesforce-destination:local` |
| `plet-salesforce-source` | `source/http/plet-salesforce-source` | `dashflow/plet-salesforce-source:local` |
| `plet-sample-filter` | `transformer/filter/plet-sample-filter` | `dashflow/plet-sample-filter:local` |
| `plet-schedule-source` | `source/trigger/plet-schedule-source` | `dashflow/plet-schedule-source:local` |
| `plet-schema-validator` | `transformer/filter/plet-schema-validator` | `dashflow/plet-schema-validator:local` |
| `plet-script-transform` | `transformer/quality/plet-script-transform` | `dashflow/plet-script-transform:local` |
| `plet-sentiment-tag` | `transformer/extract/plet-sentiment-tag` | `dashflow/plet-sentiment-tag:local` |
| `plet-sharepoint-destination` | `destination/saas/plet-sharepoint-destination` | `dashflow/plet-sharepoint-destination:local` |
| `plet-sharepoint-source` | `source/http/plet-sharepoint-source` | `dashflow/plet-sharepoint-source:local` |
| `plet-slack-destination` | `destination/notify/plet-slack-destination` | `dashflow/plet-slack-destination:local` |
| `plet-slack-source` | `source/http/plet-slack-source` | `dashflow/plet-slack-source:local` |
| `plet-snowflake-destination` | `destination/database/plet-snowflake-destination` | `dashflow/plet-snowflake-destination:local` |
| `plet-snowflake-source` | `source/database/plet-snowflake-source` | `dashflow/plet-snowflake-source:local` |
| `plet-splitter` | `transformer/structure/plet-splitter` | `dashflow/plet-splitter:local` |
| `plet-sqs-destination` | `destination/messaging/plet-sqs-destination` | `dashflow/plet-sqs-destination:local` |
| `plet-sqs-source` | `source/messaging/plet-sqs-source` | `dashflow/plet-sqs-source:local` |
| `plet-teams-destination` | `destination/notify/plet-teams-destination` | `dashflow/plet-teams-destination:local` |
| `plet-time-window` | `transformer/quality/plet-time-window` | `dashflow/plet-time-window:local` |
| `plet-type-coercer` | `transformer/transform/plet-type-coercer` | `dashflow/plet-type-coercer:local` |
| `plet-unit-converter` | `transformer/enrich/plet-unit-converter` | `dashflow/plet-unit-converter:local` |
| `plet-webhook-destination` | `destination/notify/plet-webhook-destination` | `dashflow/plet-webhook-destination:local` |
| `plet-webhook-source` | `source/http/plet-webhook-source` | `dashflow/plet-webhook-source:local` |
| `plet-xml-to-json` | `transformer/transform/plet-xml-to-json` | `dashflow/plet-xml-to-json:local` |
