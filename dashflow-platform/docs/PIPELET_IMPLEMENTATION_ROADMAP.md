# Pipelet + connector implementation roadmap

Living document for the sequenced implementation program (Wave A + impl spec).  
Shared contracts and wave order match the approved plan.

## Shared contracts

| Layer | Contract |
|-------|----------|
| I/O | [`pipelets/_common/io_transport.py`](../pipelets/_common/io_transport.py): `IO_MODE`, `IO_BROKER`, queues, `SOURCE_TRIGGER=once` for sources |
| Config | `_common/config_merge.py` + `CONNECTOR_CONFIG` / `SERVICE_CONFIG` / `DEPLOYMENT_CONFIG` / `EXECUTION_CONFIG` |
| Metadata | `pipelets/.../<id>/pipelet.json` (see PATHS.json) + [`dashflow-ui/src/fixtures/pipelets.json`](../dashflow-ui/src/fixtures/pipelets.json) |
| Binding | Step `connector_refs` → API injects `CONNECTOR_CONFIG` |
| Image | `dashflow/<pipeletId>:<tag>` |

### Implementation-spec template (every pipelet)

1. Purpose + category  
2. `connectorType` + required connector JSON fields  
3. Config schema (`requiredDeploymentKeys` / `requiredExecutionKeys` + defaults)  
4. I/O contract (in → out)  
5. Layout (`Dockerfile`, `main.py`, domain module, `pipelet.json`, tests, `README.md`)  
6. Extra env beyond platform defaults  
7. Acceptance: unit tests + stdio smoke + sample/KB  
8. Catalog: `active: true`, [`REGISTRY.md`](../pipelets/REGISTRY.md), `localdev.sh` / ACR push lists  

### Connector platform

| Kind | ID | Status |
|------|-----|--------|
| rest | `ct-rest` | Seeded |
| storage | `ct-storage` | Seeded; Azure Blob/ADLS fields via Flyway extension |
| message_bus | `ct-message-bus` | Seeded |
| event_listener | `ct-event-listener` | Seeded |
| db | `ct-db` | Seeded (Wave 0) — `jdbcUrl`, `username`, `password`, `driver`, vendor extras |
| grpc | — | Enum only (future) |

---

## Wave 0 — Harden runnable pipelets

| ID | connectorType | Status notes |
|----|---------------|--------------|
| `plet-csv-source` | storage | `active: true`; Azure endpoint docs |
| `plet-s3-source` | storage | Fixture `connectorType`; Azure-compatible storage |
| `plet-rest-source` | rest | Non-2xx errors; SERVICE_CONFIG auth |
| `plet-csv-to-json` | none | Golden stdio tests |
| `plet-field-mapper` | none | Mapping unit tests |
| `plet-python-filter` | none | Filter tests + threat-model note |
| `plet-webhook-destination` | rest | Declare `connectorType` in pipelet.json |

### Specs (Wave 0)

#### `plet-csv-source`
- **Purpose:** Fetch delimited object from storage → `records[]`
- **Connector:** `storage` — `bucket`, `endpoint?`, `region`, creds; Azure: `account`, `container`, `endpoint`
- **Execution:** `objectKey` (req), `delimiter`, `hasHeader`, `batchSize`
- **I/O:** `SOURCE_TRIGGER=once`; emit `{ records, recordCount }`
- **Acceptance:** `csv-source-demo.pipeline.json`

#### `plet-s3-source`
- **Purpose:** Fetch single object → payload/body
- **Connector:** `storage` (same schema)
- **Execution:** `objectKey` (req)
- **I/O:** emit `{ payload, contentType?, objectKey }`

#### `plet-rest-source`
- **Purpose:** HTTP GET/POST `baseUrl`+`path` → JSON payload/records
- **Connector:** `rest` — `baseUrl`; optional headers/timeout; auth via `SERVICE_CONFIG`
- **Execution:** `path` (req), `method`, `query`, `headers`, `timeoutMs`

#### `plet-csv-to-json` / `plet-field-mapper` / `plet-python-filter`
- **Connector:** none  
- **I/O:** consume stage message → transform → publish  
- Processors ignore `run-*` kickoffs where applicable  

#### `plet-webhook-destination`
- **Purpose:** POST JSON to REST sink  
- **Connector:** `rest` (declare)  
- **Execution:** `path`, `method`, `bodyKey`, `timeoutMs`  

---

## Wave 1 — Highest-value new I/O

| # | ID | Category | connectorType |
|---|-----|----------|---------------|
| 1 | `plet-webhook-source` | Source | event_listener |
| 2 | `plet-http-poll-source` | Source | rest |
| 3 | `plet-jdbc-source` | Source | db |
| 4 | `plet-event-hub-source` | Source | message_bus |
| 5 | `plet-s3-destination` | Destination | storage |
| 6 | `plet-azure-blob-destination` | Destination | storage |
| 7 | `plet-adls-destination` | Destination | storage |
| 8 | `plet-jdbc-destination` | Destination | db |
| 9 | `plet-null-destination` | Destination | none |
| 10 | `plet-json-transform` | Processor | none |
| 11 | `plet-schema-validator` | Processor | none |
| 12 | `plet-null-drop` | Processor | none |
| 13 | `plet-deduplicator` | Processor | none |

### Wave 1 impl specs (summary)

**`plet-webhook-source`** — Consume platform webhook queue / event payload; emit `{ payload, headers? }`. Execution: `path` filter optional.  

**`plet-http-poll-source`** — REST poll once per Job (`SOURCE_TRIGGER=once`); execution `path`, `intervalMs` (informational for once-mode).  

**`plet-jdbc-source`** — `db` connector; execution `query` (req), `incrementalKey?`; emit `records`.  

**`plet-event-hub-source`** — `message_bus`; execution `hubName`, `consumerGroup`, `maxEvents`; emit `records`/`payload`.  

**`plet-s3-destination` / `plet-azure-blob-destination` / `plet-adls-destination`** — storage put; execution `objectKey` template; body from `payload` or `records` JSON.  

**`plet-jdbc-destination`** — `db`; execution `table` or `statement`, `mode=insert|upsert`.  

**`plet-null-destination`** — ack and drop; log recordCount.  

**`plet-json-transform`** — execution `expression` (dot-path assigns) or `template` JSON.  

**`plet-schema-validator`** — execution `schema` (JSON Schema object/string); `onFail=error|drop`.  

**`plet-null-drop`** — drop null/empty fields or records.  

**`plet-deduplicator`** — execution `keyFields` (comma list); keep first.  

---

## Wave 2 — Remaining sources (order)

1. `plet-kafka-source` (message_bus)  
2. `plet-sqs-source` (message_bus)  
3. `plet-amqp-source` (message_bus)  
4. `plet-pub-sub-source` (message_bus)  
5. `plet-mongodb-source` (db)  
6. `plet-snowflake-source` (db)  
7. `plet-bigquery-source` (db)  
8. `plet-ftp-source` (storage)  
9. `plet-file-watch-source` (none)  
10. `plet-avro-source` (storage)  
11. `plet-parquet-source` (storage)  
12. `plet-graphql-source` (rest)  
13. `plet-imap-source` (rest)  
14. `plet-iot-hub-source` (message_bus)  
15. `plet-cdc-source` (db)  
16. `plet-schedule-source` (none)  
17. `plet-manual-source` (none)  
18. `plet-redis-stream-source` (message_bus)  
19. `plet-nats-source` (message_bus)  
20. `plet-sharepoint-source` (rest)  
21. `plet-dropbox-source` (rest)  
22. `plet-onedrive-source` (rest)  
23. `plet-slack-source` (rest)  
24. `plet-salesforce-source` (rest)  
25. `plet-gcs-source` (storage)  

Each: scaffold under `pipelets/{source|transformer|destination}/<group>/<id>/`, `active: true`, REGISTRY entry. Behavior: validate connector/config, emit structured `{ records|payload, pipeletId, stub: false }` using connector when applicable (HTTP/storage/db drivers where deps allow; otherwise validated passthrough with clear log).

---

## Wave 3 — Remaining processors (order)

`plet-json-path` → `plet-xml-to-json` → `plet-avro-codec` → `plet-type-coercer` → `plet-flatten` → `plet-splitter` → `plet-joiner` → `plet-aggregator` → `plet-sample-filter` → `plet-masker` → `plet-hasher` → `plet-encryption` → `plet-compression` → `plet-enricher` → `plet-lookup-cache` → `plet-geo-enrich` → `plet-currency-fx` → `plet-unit-converter` → `plet-regex-extract` → `plet-entity-extract` → `plet-html-strip` → `plet-language-detect` → `plet-sentiment-tag` → `plet-anomaly-flag` → `plet-ml-scorer` → `plet-script-transform` → `plet-branch-router` → `plet-rate-limiter` → `plet-retry-buffer` → `plet-time-window`

Default: no connector; transform `records`/`payload` per execution config keys documented in `pipelet.json`.

---

## Wave 4 — Remaining destinations (groups)

| Group | IDs | connectorType |
|-------|-----|---------------|
| Object store | `plet-gcs-destination`, `plet-file-destination`, `plet-archive-destination`, `plet-ftp-destination` | storage |
| Messaging | `plet-kafka-destination`, `plet-sqs-destination`, `plet-event-hub-destination`, `plet-pub-sub-destination` | message_bus |
| Data stores | `plet-mongodb-destination`, `plet-elasticsearch-destination`, `plet-opensearch-destination`, `plet-snowflake-destination`, `plet-bigquery-destination`, `plet-redshift-destination`, `plet-databricks-destination`, `plet-clickhouse-destination`, `plet-dynamodb-destination`, `plet-cosmos-db-destination`, `plet-redis-destination` | db |
| SaaS / notify | `plet-email-destination`, `plet-slack-destination`, `plet-teams-destination`, `plet-salesforce-destination`, `plet-hubspot-destination`, `plet-pagerduty-destination`, `plet-datadog-destination`, `plet-prometheus-push` | rest |
| Files SaaS | `plet-sharepoint-destination`, `plet-dropbox-destination`, `plet-onedrive-destination` | rest |

---

## Cadence

1. One pipelet per change set when adding complex I/O.  
2. Fill/update this doc section → implement → activate → REGISTRY/ACR/`localdev.sh`.  
3. Tests + sample or KB note.  

## Checklist status

- [x] Template + this roadmap
- [x] Wave 0 harden
- [x] `ct-db` + storage Azure schema (`V22`)
- [x] Wave 1 implementations (generated + logic behaviors)
- [x] Wave 2 sources
- [x] Wave 3 processors
- [x] Wave 4 destinations

Regenerate scaffolds anytime:

```bash
python3 scripts/generate_catalog_pipelets.py
```
