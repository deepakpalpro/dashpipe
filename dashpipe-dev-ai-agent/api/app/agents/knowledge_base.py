"""Dataflow pipelet catalog and pipeline patterns knowledge base."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

_KNOWLEDGE_DIR = Path(__file__).resolve().parent.parent / "knowledge"


class KnowledgeBase:
    def __init__(self, knowledge_dir: Path | None = None) -> None:
        root = knowledge_dir or _KNOWLEDGE_DIR
        self.pipeline_rules = self._read_text(root / "pipeline-rules.md")
        self.catalog_core = self._read_json_array(root / "catalog-core.json")
        self.patterns = self._read_json_array(root / "patterns.json")
        self.catalog_by_id: dict[str, dict[str, Any]] = {
            item["id"]: item for item in self.catalog_core if item.get("id")
        }

    @staticmethod
    def _read_text(path: Path) -> str:
        if not path.is_file():
            return ""
        return path.read_text(encoding="utf-8").strip()

    @staticmethod
    def _read_json_array(path: Path) -> list[dict[str, Any]]:
        if not path.is_file():
            return []
        data = json.loads(path.read_text(encoding="utf-8"))
        return data if isinstance(data, list) else []

    def known_pipelet_ids(self) -> set[str]:
        return set(self.catalog_by_id.keys())

    def catalog_entry(self, pipelet_id: str) -> dict[str, Any] | None:
        return self.catalog_by_id.get(pipelet_id)

    def compact_prompt_context(self) -> str:
        lines = [
            "RULES:",
            "- Linear Source→Processor→Destination; 2–5 steps.",
            "- Prefer queue ioMode. Activate before Run/schedule.",
            "- Manual: plet-manual-source. Schedule: plet-schedule-source + scheduleCron.",
            "- S3 dest ≠ file dest. Leave connector_ids/service_ids empty.",
            "",
            "CATALOG (id|category|blurb|exec|dep):",
        ]
        for node in self.catalog_core:
            pid = node.get("id")
            if not pid:
                continue
            lines.append(
                f"{pid}|{node.get('category', '')}|{(node.get('blurb') or '')[:70]}|"
                f"{json.dumps(node.get('execution_config') or {}, separators=(',', ':'))}|"
                f"{json.dumps(node.get('deployment_config') or {}, separators=(',', ':'))}"
            )
        lines.append("")
        lines.append("PATTERNS (id: steps):")
        for pattern in self.patterns:
            pid = pattern.get("id")
            steps = pattern.get("steps") or []
            if pid and isinstance(steps, list):
                step_str = ">".join(str(s) for s in steps)
                lines.append(f"{pid}:{step_str}")
        return "\n".join(lines)
