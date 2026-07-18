from __future__ import annotations

import json
from pathlib import Path
from typing import Any


class PipeletCatalog:
    def __init__(self, catalog_path: Path):
        self.catalog_path = catalog_path
        self._pipelets: list[dict[str, Any]] | None = None

    def _load(self) -> list[dict[str, Any]]:
        if self._pipelets is None:
            raw = json.loads(self.catalog_path.read_text(encoding="utf-8"))
            if not isinstance(raw, list):
                raise ValueError(f"Expected pipelet catalog array in {self.catalog_path}")
            self._pipelets = raw
        return self._pipelets

    def list_pipelets(
        self,
        *,
        category: str | None = None,
        active_only: bool = False,
        query: str | None = None,
        limit: int = 200,
    ) -> list[dict[str, Any]]:
        items = self._load()
        if category:
            cat = category.lower()
            items = [p for p in items if str(p.get("category", "")).lower() == cat]
        if active_only:
            items = [p for p in items if p.get("active") is True]
        if query:
            q = query.lower()
            items = [
                p
                for p in items
                if q in str(p.get("id", "")).lower()
                or q in str(p.get("name", "")).lower()
                or q in str(p.get("description", "")).lower()
            ]
        return items[: max(1, min(limit, 500))]

    def get_pipelet(self, pipelet_id: str) -> dict[str, Any] | None:
        for pipelet in self._load():
            if pipelet.get("id") == pipelet_id:
                return pipelet
        return None
