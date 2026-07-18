"""Tests for shared platform_ops."""

from __future__ import annotations

from unittest.mock import MagicMock

import pytest

from dashpipe_mcp.config import Settings
from dashpipe_mcp.platform_ops import PlatformOps


@pytest.fixture
def ops() -> PlatformOps:
    return PlatformOps(Settings(api_url="http://test", tenant_id="T001"))


def test_dashpipe_health_delegates(ops: PlatformOps):
    client = MagicMock()
    client.health.return_value = {"status": "UP"}
    ops._client = client
    assert ops.dashpipe_health() == {"status": "UP"}
    client.health.assert_called_once()


def test_activate_pipeline_sets_active(ops: PlatformOps):
    client = MagicMock()
    client.get_pipeline.return_value = {
        "name": "Demo",
        "visibility": "PRIVATE",
        "execution_mode": "ASYNC",
        "deployment_config": {},
        "execution_config": {},
    }
    client.update_pipeline.return_value = {"id": "p1", "status": "ACTIVE"}
    ops._client = client

    result = ops.activate_pipeline("p1")
    assert result["status"] == "ACTIVE"
    body = client.update_pipeline.call_args[0][1]
    assert body["status"] == "ACTIVE"


def test_debug_execution_composite(ops: PlatformOps):
    client = MagicMock()
    client.get_execution.return_value = {"id": "e1"}
    client.execution_logs.return_value = {"logs": []}
    client.pipeline_errors.return_value = []
    client.pipeline_completeness.return_value = {"score": 1}
    client.observability_links.return_value = {"grafana": "http://g"}
    ops._client = client

    result = ops.debug_execution("p1", "e1")
    assert result["execution"]["id"] == "e1"
    assert "logs" in result
    assert "links" in result
