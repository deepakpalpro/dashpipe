# Pipelet runtime registry


Catalog pipelets live under `pipelets/{source|transformer|destination}/<group>/<id>/`.
Image tags stay flat (`dashpipe/<id>:local`). Path index: [`PATHS.json`](PATHS.json).

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
| `plet-s3-source` | `source/storage/plet-s3-source` | `dashpipe/plet-s3-source:local` |
| `plet-csv-source` | `source/storage/plet-csv-source` | `dashpipe/plet-csv-source:local` |
| `plet-rest-source` | `source/http/plet-rest-source` | `dashpipe/plet-rest-source:local` |
| `plet-csv-to-json` | `transformer/transform/plet-csv-to-json` | `dashpipe/plet-csv-to-json:local` |
| `plet-python-filter` | `transformer/filter/plet-python-filter` | `dashpipe/plet-python-filter:local` |
| `plet-field-mapper` | `transformer/transform/plet-field-mapper` | `dashpipe/plet-field-mapper:local` |
| `plet-webhook-destination` | `destination/notify/plet-webhook-destination` | `dashpipe/plet-webhook-destination:local` |
| `inventory-batch` | `inventory/` | `dashpipe/inventory-pipelet:local` |

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
docker build -f "pipelets/$rel/Dockerfile" -t "dashpipe/${id}:local" pipelets
```

## All catalog images

| ID | Path | Image |
|----|------|-------|
| `plet-adls-destination` | `destination/storage/plet-adls-destination` | `dashpipe/plet-adls-destination:local` |
| `plet-aggregator` | `transformer/structure/plet-aggregator` | `dashpipe/plet-aggregator:local` |
| `plet-amqp-source` | `source/messaging/plet-amqp-source` | `dashpipe/plet-amqp-source:local` |
| `plet-anomaly-flag` | `transformer/quality/plet-anomaly-flag` | `dashpipe/plet-anomaly-flag:local` |
| `plet-archive-destination` | `destination/storage/plet-archive-destination` | `dashpipe/plet-archive-destination:local` |
| `plet-avro-codec` | `transformer/transform/plet-avro-codec` | `dashpipe/plet-avro-codec:local` |
| `plet-avro-source` | `source/storage/plet-avro-source` | `dashpipe/plet-avro-source:local` |
| `plet-azure-blob-destination` | `destination/storage/plet-azure-blob-destination` | `dashpipe/plet-azure-blob-destination:local` |
| `plet-bigquery-destination` | `destination/database/plet-bigquery-destination` | `dashpipe/plet-bigquery-destination:local` |
| `plet-bigquery-source` | `source/database/plet-bigquery-source` | `dashpipe/plet-bigquery-source:local` |
| `plet-branch-router` | `transformer/structure/plet-branch-router` | `dashpipe/plet-branch-router:local` |
| `plet-cdc-source` | `source/database/plet-cdc-source` | `dashpipe/plet-cdc-source:local` |
| `plet-clickhouse-destination` | `destination/database/plet-clickhouse-destination` | `dashpipe/plet-clickhouse-destination:local` |
| `plet-compression` | `transformer/security/plet-compression` | `dashpipe/plet-compression:local` |
| `plet-cosmos-db-destination` | `destination/database/plet-cosmos-db-destination` | `dashpipe/plet-cosmos-db-destination:local` |
| `plet-csv-source` | `source/storage/plet-csv-source` | `dashpipe/plet-csv-source:local` |
| `plet-csv-to-json` | `transformer/transform/plet-csv-to-json` | `dashpipe/plet-csv-to-json:local` |
| `plet-currency-fx` | `transformer/enrich/plet-currency-fx` | `dashpipe/plet-currency-fx:local` |
| `plet-databricks-destination` | `destination/database/plet-databricks-destination` | `dashpipe/plet-databricks-destination:local` |
| `plet-datadog-destination` | `destination/notify/plet-datadog-destination` | `dashpipe/plet-datadog-destination:local` |
| `plet-deduplicator` | `transformer/filter/plet-deduplicator` | `dashpipe/plet-deduplicator:local` |
| `plet-dropbox-destination` | `destination/saas/plet-dropbox-destination` | `dashpipe/plet-dropbox-destination:local` |
| `plet-dropbox-source` | `source/http/plet-dropbox-source` | `dashpipe/plet-dropbox-source:local` |
| `plet-dynamodb-destination` | `destination/database/plet-dynamodb-destination` | `dashpipe/plet-dynamodb-destination:local` |
| `plet-elasticsearch-destination` | `destination/database/plet-elasticsearch-destination` | `dashpipe/plet-elasticsearch-destination:local` |
| `plet-email-destination` | `destination/notify/plet-email-destination` | `dashpipe/plet-email-destination:local` |
| `plet-encryption` | `transformer/security/plet-encryption` | `dashpipe/plet-encryption:local` |
| `plet-enricher` | `transformer/enrich/plet-enricher` | `dashpipe/plet-enricher:local` |
| `plet-entity-extract` | `transformer/extract/plet-entity-extract` | `dashpipe/plet-entity-extract:local` |
| `plet-event-hub-destination` | `destination/messaging/plet-event-hub-destination` | `dashpipe/plet-event-hub-destination:local` |
| `plet-event-hub-source` | `source/messaging/plet-event-hub-source` | `dashpipe/plet-event-hub-source:local` |
| `plet-field-mapper` | `transformer/transform/plet-field-mapper` | `dashpipe/plet-field-mapper:local` |
| `plet-file-destination` | `destination/storage/plet-file-destination` | `dashpipe/plet-file-destination:local` |
| `plet-file-watch-source` | `source/storage/plet-file-watch-source` | `dashpipe/plet-file-watch-source:local` |
| `plet-flatten` | `transformer/transform/plet-flatten` | `dashpipe/plet-flatten:local` |
| `plet-ftp-destination` | `destination/storage/plet-ftp-destination` | `dashpipe/plet-ftp-destination:local` |
| `plet-ftp-source` | `source/storage/plet-ftp-source` | `dashpipe/plet-ftp-source:local` |
| `plet-gcs-destination` | `destination/storage/plet-gcs-destination` | `dashpipe/plet-gcs-destination:local` |
| `plet-gcs-source` | `source/storage/plet-gcs-source` | `dashpipe/plet-gcs-source:local` |
| `plet-geo-enrich` | `transformer/enrich/plet-geo-enrich` | `dashpipe/plet-geo-enrich:local` |
| `plet-graphql-source` | `source/http/plet-graphql-source` | `dashpipe/plet-graphql-source:local` |
| `plet-hasher` | `transformer/security/plet-hasher` | `dashpipe/plet-hasher:local` |
| `plet-html-strip` | `transformer/extract/plet-html-strip` | `dashpipe/plet-html-strip:local` |
| `plet-http-poll-source` | `source/http/plet-http-poll-source` | `dashpipe/plet-http-poll-source:local` |
| `plet-hubspot-destination` | `destination/saas/plet-hubspot-destination` | `dashpipe/plet-hubspot-destination:local` |
| `plet-imap-source` | `source/http/plet-imap-source` | `dashpipe/plet-imap-source:local` |
| `plet-iot-hub-source` | `source/messaging/plet-iot-hub-source` | `dashpipe/plet-iot-hub-source:local` |
| `plet-jdbc-destination` | `destination/database/plet-jdbc-destination` | `dashpipe/plet-jdbc-destination:local` |
| `plet-jdbc-source` | `source/database/plet-jdbc-source` | `dashpipe/plet-jdbc-source:local` |
| `plet-joiner` | `transformer/structure/plet-joiner` | `dashpipe/plet-joiner:local` |
| `plet-json-path` | `transformer/transform/plet-json-path` | `dashpipe/plet-json-path:local` |
| `plet-json-transform` | `transformer/transform/plet-json-transform` | `dashpipe/plet-json-transform:local` |
| `plet-kafka-destination` | `destination/messaging/plet-kafka-destination` | `dashpipe/plet-kafka-destination:local` |
| `plet-kafka-source` | `source/messaging/plet-kafka-source` | `dashpipe/plet-kafka-source:local` |
| `plet-language-detect` | `transformer/extract/plet-language-detect` | `dashpipe/plet-language-detect:local` |
| `plet-lookup-cache` | `transformer/enrich/plet-lookup-cache` | `dashpipe/plet-lookup-cache:local` |
| `plet-manual-source` | `source/trigger/plet-manual-source` | `dashpipe/plet-manual-source:local` |
| `plet-masker` | `transformer/security/plet-masker` | `dashpipe/plet-masker:local` |
| `plet-ml-scorer` | `transformer/quality/plet-ml-scorer` | `dashpipe/plet-ml-scorer:local` |
| `plet-mongodb-destination` | `destination/database/plet-mongodb-destination` | `dashpipe/plet-mongodb-destination:local` |
| `plet-mongodb-source` | `source/database/plet-mongodb-source` | `dashpipe/plet-mongodb-source:local` |
| `plet-nats-source` | `source/messaging/plet-nats-source` | `dashpipe/plet-nats-source:local` |
| `plet-null-destination` | `destination/util/plet-null-destination` | `dashpipe/plet-null-destination:local` |
| `plet-null-drop` | `transformer/filter/plet-null-drop` | `dashpipe/plet-null-drop:local` |
| `plet-onedrive-destination` | `destination/saas/plet-onedrive-destination` | `dashpipe/plet-onedrive-destination:local` |
| `plet-onedrive-source` | `source/http/plet-onedrive-source` | `dashpipe/plet-onedrive-source:local` |
| `plet-opensearch-destination` | `destination/database/plet-opensearch-destination` | `dashpipe/plet-opensearch-destination:local` |
| `plet-pagerduty-destination` | `destination/notify/plet-pagerduty-destination` | `dashpipe/plet-pagerduty-destination:local` |
| `plet-parquet-source` | `source/storage/plet-parquet-source` | `dashpipe/plet-parquet-source:local` |
| `plet-prometheus-push` | `destination/notify/plet-prometheus-push` | `dashpipe/plet-prometheus-push:local` |
| `plet-pub-sub-destination` | `destination/messaging/plet-pub-sub-destination` | `dashpipe/plet-pub-sub-destination:local` |
| `plet-pub-sub-source` | `source/messaging/plet-pub-sub-source` | `dashpipe/plet-pub-sub-source:local` |
| `plet-python-filter` | `transformer/filter/plet-python-filter` | `dashpipe/plet-python-filter:local` |
| `plet-rate-limiter` | `transformer/quality/plet-rate-limiter` | `dashpipe/plet-rate-limiter:local` |
| `plet-redis-destination` | `destination/database/plet-redis-destination` | `dashpipe/plet-redis-destination:local` |
| `plet-redis-stream-source` | `source/messaging/plet-redis-stream-source` | `dashpipe/plet-redis-stream-source:local` |
| `plet-redshift-destination` | `destination/database/plet-redshift-destination` | `dashpipe/plet-redshift-destination:local` |
| `plet-regex-extract` | `transformer/extract/plet-regex-extract` | `dashpipe/plet-regex-extract:local` |
| `plet-rest-source` | `source/http/plet-rest-source` | `dashpipe/plet-rest-source:local` |
| `plet-retry-buffer` | `transformer/quality/plet-retry-buffer` | `dashpipe/plet-retry-buffer:local` |
| `plet-s3-destination` | `destination/storage/plet-s3-destination` | `dashpipe/plet-s3-destination:local` |
| `plet-s3-source` | `source/storage/plet-s3-source` | `dashpipe/plet-s3-source:local` |
| `plet-salesforce-destination` | `destination/saas/plet-salesforce-destination` | `dashpipe/plet-salesforce-destination:local` |
| `plet-salesforce-source` | `source/http/plet-salesforce-source` | `dashpipe/plet-salesforce-source:local` |
| `plet-sample-filter` | `transformer/filter/plet-sample-filter` | `dashpipe/plet-sample-filter:local` |
| `plet-schedule-source` | `source/trigger/plet-schedule-source` | `dashpipe/plet-schedule-source:local` |
| `plet-schema-validator` | `transformer/filter/plet-schema-validator` | `dashpipe/plet-schema-validator:local` |
| `plet-script-transform` | `transformer/quality/plet-script-transform` | `dashpipe/plet-script-transform:local` |
| `plet-sentiment-tag` | `transformer/extract/plet-sentiment-tag` | `dashpipe/plet-sentiment-tag:local` |
| `plet-sharepoint-destination` | `destination/saas/plet-sharepoint-destination` | `dashpipe/plet-sharepoint-destination:local` |
| `plet-sharepoint-source` | `source/http/plet-sharepoint-source` | `dashpipe/plet-sharepoint-source:local` |
| `plet-slack-destination` | `destination/notify/plet-slack-destination` | `dashpipe/plet-slack-destination:local` |
| `plet-slack-source` | `source/http/plet-slack-source` | `dashpipe/plet-slack-source:local` |
| `plet-snowflake-destination` | `destination/database/plet-snowflake-destination` | `dashpipe/plet-snowflake-destination:local` |
| `plet-snowflake-source` | `source/database/plet-snowflake-source` | `dashpipe/plet-snowflake-source:local` |
| `plet-splitter` | `transformer/structure/plet-splitter` | `dashpipe/plet-splitter:local` |
| `plet-sqs-destination` | `destination/messaging/plet-sqs-destination` | `dashpipe/plet-sqs-destination:local` |
| `plet-sqs-source` | `source/messaging/plet-sqs-source` | `dashpipe/plet-sqs-source:local` |
| `plet-teams-destination` | `destination/notify/plet-teams-destination` | `dashpipe/plet-teams-destination:local` |
| `plet-time-window` | `transformer/quality/plet-time-window` | `dashpipe/plet-time-window:local` |
| `plet-type-coercer` | `transformer/transform/plet-type-coercer` | `dashpipe/plet-type-coercer:local` |
| `plet-unit-converter` | `transformer/enrich/plet-unit-converter` | `dashpipe/plet-unit-converter:local` |
| `plet-webhook-destination` | `destination/notify/plet-webhook-destination` | `dashpipe/plet-webhook-destination:local` |
| `plet-webhook-source` | `source/http/plet-webhook-source` | `dashpipe/plet-webhook-source:local` |
| `plet-xml-to-json` | `transformer/transform/plet-xml-to-json` | `dashpipe/plet-xml-to-json:local` |
