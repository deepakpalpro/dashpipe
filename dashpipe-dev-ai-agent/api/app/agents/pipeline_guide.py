"""Design Dataflow pipelines via LangGraph + LangChain."""

from __future__ import annotations

from app.agents.knowledge_base import KnowledgeBase
from app.agents.pipeline_parser import kb_fallback
from app.graphs.pipeline_guide_graph import run_pipeline_guide
from app.platform.catalog_loader import load_live_catalog
from app.platform.settings import PlatformSettings
from app.schemas import CatalogPipeletSummary, PipelineGuideRequest, PipelineGuideResponse


class PipelineGuideService:
    def __init__(self, kb: KnowledgeBase | None = None) -> None:
        self._kb = kb or KnowledgeBase()

    def guide(
        self,
        request: PipelineGuideRequest,
        platform_settings: PlatformSettings | None = None,
    ) -> PipelineGuideResponse:
        enriched = self._enrich_catalog(request, platform_settings)
        return run_pipeline_guide(enriched, self._kb)

    def _enrich_catalog(
        self,
        request: PipelineGuideRequest,
        platform_settings: PlatformSettings | None,
    ) -> PipelineGuideRequest:
        if request.catalog:
            return request
        live = load_live_catalog(platform_settings)
        if not live:
            static = [
                CatalogPipeletSummary(
                    id=item["id"],
                    name=item.get("name"),
                    category=item.get("category"),
                    description=item.get("description"),
                    required_execution_keys=item.get("required_execution_keys") or [],
                )
                for item in self._kb.catalog_core
                if item.get("id")
            ]
            return request.model_copy(update={"catalog": static})
        return request.model_copy(update={"catalog": live})

    def _fallback(self, message: str) -> PipelineGuideResponse | None:
        return kb_fallback(self._kb, message)
