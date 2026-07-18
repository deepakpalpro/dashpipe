"""Design Dataflow pipelines via LangGraph + LangChain."""

from __future__ import annotations

from app.agents.knowledge_base import KnowledgeBase
from app.agents.pipeline_parser import kb_fallback
from app.graphs.pipeline_guide_graph import run_pipeline_guide
from app.schemas import PipelineGuideRequest, PipelineGuideResponse


class PipelineGuideService:
    def __init__(self, kb: KnowledgeBase | None = None) -> None:
        self._kb = kb or KnowledgeBase()

    def guide(self, request: PipelineGuideRequest) -> PipelineGuideResponse:
        return run_pipeline_guide(request, self._kb)

    def _fallback(self, message: str) -> PipelineGuideResponse | None:
        return kb_fallback(self._kb, message)
