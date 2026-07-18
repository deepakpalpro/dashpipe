#!/usr/bin/env python3
"""Unit tests for plet-s3-source config merge / required Keys."""

from __future__ import annotations

import unittest

from config import resolve_config


class ResolveConfigTest(unittest.TestCase):
    def test_merges_connector_service_and_step_keys(self) -> None:
        cfg = resolve_config(
            connector={
                "bucket": "demo-s3-source",
                "endpoint": "http://localstack:4566",
                "accessKeyId": "from-connector",
                "secretAccessKey": "conn-secret",
            },
            service={"accessKeyId": "from-service", "secretAccessKey": "svc-secret"},
            deployment={"region": "us-east-1"},
            execution={"objectKey": "inventory/daily.csv"},
            pipelet_deployment_defaults={"cloud": "aws", "region": ""},
            pipelet_execution_defaults={"objectKey": "", "batchSize": "1"},
        )
        self.assertEqual(cfg.bucket, "demo-s3-source")
        self.assertEqual(cfg.object_key, "inventory/daily.csv")
        self.assertEqual(cfg.region, "us-east-1")
        self.assertEqual(cfg.endpoint, "http://localstack:4566")
        self.assertEqual(cfg.access_key_id, "from-service")
        self.assertEqual(cfg.secret_access_key, "svc-secret")

    def test_accepts_key_alias_for_object_key(self) -> None:
        cfg = resolve_config(
            connector={"bucket": "b"},
            deployment={"region": "eu-west-1"},
            execution={"key": "path/file.csv"},
        )
        self.assertEqual(cfg.object_key, "path/file.csv")

    def test_step_overrides_connector_region(self) -> None:
        cfg = resolve_config(
            connector={"bucket": "b", "region": "us-east-1"},
            deployment={"region": "ap-southeast-2"},
            execution={"objectKey": "a.csv"},
        )
        self.assertEqual(cfg.region, "ap-southeast-2")

    def test_missing_required_keys_fail(self) -> None:
        with self.assertRaises(SystemExit) as ctx:
            resolve_config(connector={}, deployment={}, execution={})
        msg = str(ctx.exception)
        self.assertIn("deployment.region", msg)
        self.assertIn("execution.objectKey", msg)
        self.assertIn("connector.bucket", msg)

    def test_empty_string_defaults_do_not_satisfy_required(self) -> None:
        with self.assertRaises(SystemExit):
            resolve_config(
                connector={"bucket": "b"},
                deployment={"region": ""},
                execution={"objectKey": ""},
                pipelet_deployment_defaults={"region": ""},
                pipelet_execution_defaults={"objectKey": ""},
            )


if __name__ == "__main__":
    unittest.main()
