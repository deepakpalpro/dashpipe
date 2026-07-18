import json
from pathlib import Path

import pytest

from dashpipe_mcp.catalog import PipeletCatalog


@pytest.fixture
def catalog_path(tmp_path: Path) -> Path:
    data = [
        {
            "id": "plet-rest-source",
            "name": "REST Source",
            "category": "Source",
            "active": True,
            "description": "HTTP source",
        },
        {
            "id": "plet-json-transform",
            "name": "JSON Transform",
            "category": "Processor",
            "active": True,
            "description": "Transform JSON",
        },
        {
            "id": "plet-s3-destination",
            "name": "S3 Destination",
            "category": "Destination",
            "active": False,
            "description": "Write to S3",
        },
    ]
    path = tmp_path / "pipelets.json"
    path.write_text(json.dumps(data), encoding="utf-8")
    return path


def test_list_pipelets_filters(catalog_path: Path) -> None:
    catalog = PipeletCatalog(catalog_path)
    assert len(catalog.list_pipelets(category="Source")) == 1
    assert len(catalog.list_pipelets(active_only=True)) == 2
    assert len(catalog.list_pipelets(query="s3")) == 1


def test_get_pipelet(catalog_path: Path) -> None:
    catalog = PipeletCatalog(catalog_path)
    assert catalog.get_pipelet("plet-rest-source")["name"] == "REST Source"
    assert catalog.get_pipelet("missing") is None
