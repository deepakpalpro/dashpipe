"""FastAPI application for AI Dataflow Developer."""

from __future__ import annotations

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.agents.model_catalog import code_model_catalog_text, list_models
from app.agents.pipeline_guide import PipelineGuideService
from app.agents.python_developer import PythonDeveloperService
from app.llm.exceptions import LlmUnavailableError
from app.schemas import (
    AgentModelsResponse,
    PipelineGuideRequest,
    PipelineGuideResponse,
    PythonDeveloperRequest,
    PythonDeveloperResponse,
)


def create_app() -> FastAPI:
    guide = PipelineGuideService()
    developer = PythonDeveloperService()

    app = FastAPI(
        title="AI Dataflow Developer",
        description="Pipeline design and pipelet Python/K8s generation for Dataflow (LangChain + LangGraph)",
        version="1.1.0",
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    @app.get("/api/v1/models", response_model=AgentModelsResponse)
    def models() -> AgentModelsResponse:
        return list_models()

    @app.post("/api/v1/pipeline-guide", response_model=PipelineGuideResponse)
    def pipeline_guide(body: PipelineGuideRequest) -> PipelineGuideResponse:
        try:
            return guide.guide(body)
        except LlmUnavailableError as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc

    @app.get("/api/v1/python-developer/models")
    def python_dev_models() -> dict[str, str]:
        return {"catalog": code_model_catalog_text()}

    @app.post("/api/v1/python-developer", response_model=PythonDeveloperResponse)
    def python_developer(body: PythonDeveloperRequest) -> PythonDeveloperResponse:
        try:
            return developer.generate(body)
        except LlmUnavailableError as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc

    return app


app = create_app()
