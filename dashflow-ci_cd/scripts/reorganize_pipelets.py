#!/usr/bin/env python3
"""Reorganize pipelets into source|transformer|destination/<group>/<id>/.

Run from repo root:
  python3 scripts/reorganize_pipelets.py
"""

from __future__ import annotations

import re
import shutil
from pathlib import Path

SUITE = Path(__file__).resolve().parents[2]
PLATFORM = SUITE / "dashflow-platform"
PIPELETS = PLATFORM / "pipelets"

# pipelet_id -> (top_dir, meaningful_group)
# top_dir: source | transformer | destination
LAYOUT: dict[str, tuple[str, str]] = {
    # --- sources ---
    "plet-rest-source": ("source", "http"),
    "plet-http-poll-source": ("source", "http"),
    "plet-graphql-source": ("source", "http"),
    "plet-webhook-source": ("source", "http"),
    "plet-salesforce-source": ("source", "http"),
    "plet-slack-source": ("source", "http"),
    "plet-sharepoint-source": ("source", "http"),
    "plet-dropbox-source": ("source", "http"),
    "plet-onedrive-source": ("source", "http"),
    "plet-imap-source": ("source", "http"),
    "plet-s3-source": ("source", "storage"),
    "plet-csv-source": ("source", "storage"),
    "plet-gcs-source": ("source", "storage"),
    "plet-avro-source": ("source", "storage"),
    "plet-parquet-source": ("source", "storage"),
    "plet-ftp-source": ("source", "storage"),
    "plet-file-watch-source": ("source", "storage"),
    "plet-kafka-source": ("source", "messaging"),
    "plet-sqs-source": ("source", "messaging"),
    "plet-amqp-source": ("source", "messaging"),
    "plet-pub-sub-source": ("source", "messaging"),
    "plet-event-hub-source": ("source", "messaging"),
    "plet-iot-hub-source": ("source", "messaging"),
    "plet-redis-stream-source": ("source", "messaging"),
    "plet-nats-source": ("source", "messaging"),
    "plet-jdbc-source": ("source", "database"),
    "plet-mongodb-source": ("source", "database"),
    "plet-snowflake-source": ("source", "database"),
    "plet-bigquery-source": ("source", "database"),
    "plet-cdc-source": ("source", "database"),
    "plet-schedule-source": ("source", "trigger"),
    "plet-manual-source": ("source", "trigger"),
    # --- transformers (processors) ---
    "plet-csv-to-json": ("transformer", "transform"),
    "plet-json-transform": ("transformer", "transform"),
    "plet-json-path": ("transformer", "transform"),
    "plet-xml-to-json": ("transformer", "transform"),
    "plet-avro-codec": ("transformer", "transform"),
    "plet-type-coercer": ("transformer", "transform"),
    "plet-flatten": ("transformer", "transform"),
    "plet-field-mapper": ("transformer", "transform"),
    "plet-python-filter": ("transformer", "filter"),
    "plet-null-drop": ("transformer", "filter"),
    "plet-sample-filter": ("transformer", "filter"),
    "plet-deduplicator": ("transformer", "filter"),
    "plet-schema-validator": ("transformer", "filter"),
    "plet-enricher": ("transformer", "enrich"),
    "plet-lookup-cache": ("transformer", "enrich"),
    "plet-geo-enrich": ("transformer", "enrich"),
    "plet-currency-fx": ("transformer", "enrich"),
    "plet-unit-converter": ("transformer", "enrich"),
    "plet-regex-extract": ("transformer", "extract"),
    "plet-entity-extract": ("transformer", "extract"),
    "plet-html-strip": ("transformer", "extract"),
    "plet-language-detect": ("transformer", "extract"),
    "plet-sentiment-tag": ("transformer", "extract"),
    "plet-splitter": ("transformer", "structure"),
    "plet-joiner": ("transformer", "structure"),
    "plet-aggregator": ("transformer", "structure"),
    "plet-branch-router": ("transformer", "structure"),
    "plet-masker": ("transformer", "security"),
    "plet-hasher": ("transformer", "security"),
    "plet-encryption": ("transformer", "security"),
    "plet-compression": ("transformer", "security"),
    "plet-anomaly-flag": ("transformer", "quality"),
    "plet-ml-scorer": ("transformer", "quality"),
    "plet-rate-limiter": ("transformer", "quality"),
    "plet-retry-buffer": ("transformer", "quality"),
    "plet-time-window": ("transformer", "quality"),
    "plet-script-transform": ("transformer", "quality"),
    # --- destinations ---
    "plet-s3-destination": ("destination", "storage"),
    "plet-azure-blob-destination": ("destination", "storage"),
    "plet-adls-destination": ("destination", "storage"),
    "plet-gcs-destination": ("destination", "storage"),
    "plet-file-destination": ("destination", "storage"),
    "plet-archive-destination": ("destination", "storage"),
    "plet-ftp-destination": ("destination", "storage"),
    "plet-kafka-destination": ("destination", "messaging"),
    "plet-sqs-destination": ("destination", "messaging"),
    "plet-event-hub-destination": ("destination", "messaging"),
    "plet-pub-sub-destination": ("destination", "messaging"),
    "plet-jdbc-destination": ("destination", "database"),
    "plet-mongodb-destination": ("destination", "database"),
    "plet-elasticsearch-destination": ("destination", "database"),
    "plet-opensearch-destination": ("destination", "database"),
    "plet-snowflake-destination": ("destination", "database"),
    "plet-bigquery-destination": ("destination", "database"),
    "plet-redshift-destination": ("destination", "database"),
    "plet-databricks-destination": ("destination", "database"),
    "plet-clickhouse-destination": ("destination", "database"),
    "plet-dynamodb-destination": ("destination", "database"),
    "plet-cosmos-db-destination": ("destination", "database"),
    "plet-redis-destination": ("destination", "database"),
    "plet-webhook-destination": ("destination", "notify"),
    "plet-email-destination": ("destination", "notify"),
    "plet-slack-destination": ("destination", "notify"),
    "plet-teams-destination": ("destination", "notify"),
    "plet-pagerduty-destination": ("destination", "notify"),
    "plet-datadog-destination": ("destination", "notify"),
    "plet-prometheus-push": ("destination", "notify"),
    "plet-salesforce-destination": ("destination", "saas"),
    "plet-hubspot-destination": ("destination", "saas"),
    "plet-sharepoint-destination": ("destination", "saas"),
    "plet-dropbox-destination": ("destination", "saas"),
    "plet-onedrive-destination": ("destination", "saas"),
    "plet-null-destination": ("destination", "util"),
}


def rewrite_dockerfile(df: Path, rel_pkg: str, pipelet_id: str) -> None:
    """Rewrite COPY lines that referenced flat plet-*/ to new rel path."""
    text = df.read_text()
    # Replace any COPY <old> patterns with COPY <rel_pkg>/
    # Common patterns: COPY plet-foo/... or COPY plet-foo/file ...
    text2 = re.sub(
        rf"COPY {re.escape(pipelet_id)}/",
        f"COPY {rel_pkg}/",
        text,
    )
    # Also handle multi-file COPY on one line already updated partially
    if text2 == text and f"COPY {rel_pkg}/" not in text:
        # Flat leftover: COPY some files without prefix — leave
        pass
    df.write_text(text2)


def main() -> None:
    moved = 0
    missing = []
    for pid, (top, group) in LAYOUT.items():
        src = PIPELETS / pid
        if not src.is_dir():
            missing.append(pid)
            continue
        dest_parent = PIPELETS / top / group
        dest_parent.mkdir(parents=True, exist_ok=True)
        dest = dest_parent / pid
        if dest.exists():
            continue
        shutil.move(str(src), str(dest))
        rel_pkg = f"{top}/{group}/{pid}"
        df = dest / "Dockerfile"
        if df.exists():
            rewrite_dockerfile(df, rel_pkg, pid)
        # Fix main.py parent path to _common: was parents[1], now parents[3]
        main_py = dest / "main.py"
        if main_py.exists():
            mt = main_py.read_text()
            # Path(__file__).resolve().parents[1] / "_common"  -> parents[3]
            mt2 = mt.replace("parents[1] / \"_common\"", "parents[3] / \"_common\"")
            mt2 = mt2.replace("parents[1] / '_common'", "parents[3] / '_common'")
            # Also: Path(__file__).resolve().parents[1] / "_common" variants
            mt2 = re.sub(
                r"Path\(__file__\)\.resolve\(\)\.parents\[1\]\s*/\s*[\"']_common[\"']",
                'Path(__file__).resolve().parents[3] / "_common"',
                mt2,
            )
            # rest-source style: parents[1] / "_common"
            if "_COMMON = Path(__file__).resolve().parents[1]" in mt2:
                mt2 = mt2.replace(
                    "parents[1]",
                    "parents[3]",
                    1,
                )
            main_py.write_text(mt2)
        moved += 1

    # Write path index for tooling
    index = {
        pid: f"{top}/{group}/{pid}"
        for pid, (top, group) in LAYOUT.items()
        if (PIPELETS / top / group / pid).is_dir()
    }
    (PIPELETS / "PATHS.json").write_text(
        __import__("json").dumps(index, indent=2, sort_keys=True) + "\n"
    )
    print(f"moved={moved} missing={missing}")


if __name__ == "__main__":
    main()
