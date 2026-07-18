"""FastAPI application for AI Dataflow Developer."""

from __future__ import annotations

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.agents.model_catalog import code_model_catalog_text, list_models
from app.agents.ops_agent import OpsAgentService
from app.agents.pipeline_guide import PipelineGuideService
from app.agents.python_developer import PythonDeveloperService
from app.llm.exceptions import LlmUnavailableError
from app.platform.settings import PlatformSettings
from app.schemas import (
    AgentModelsResponse,
    OpsAgentRequest,
    OpsAgentResponse,
    PipelineGuideRequest,
    PipelineGuideResponse,
    PlatformStatusResponse,
    PythonDeveloperRequest,
    PythonDeveloperResponse,
)
from dashpipe_mcp.platform_ops import PlatformOps


def create_app() -> FastAPI:
    guide = PipelineGuideService()
    developer = PythonDeveloperService()
    ops = OpsAgentService()

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

    @app.get("/api/v1/platform/status", response_model=PlatformStatusResponse)
    def platform_status() -> PlatformStatusResponse:
        cfg = PlatformSettings.from_env()
        platform = PlatformOps(cfg.to_mcp_settings())
        try:
            health = platform.dashpipe_health()
            config = platform.dashpipe_config()
            return PlatformStatusResponse(
                healthy=True,
                api_url=cfg.api_url,
                tenant_id=cfg.tenant_id,
                health=health,
                config=config,
            )
        except Exception as exc:  # noqa: BLE001
            return PlatformStatusResponse(
                healthy=False,
                api_url=cfg.api_url,
                tenant_id=cfg.tenant_id,
                error=str(exc),
            )
        finally:
            platform.close()

    @app.post("/api/v1/platform-ops/chat", response_model=OpsAgentResponse)
    def platform_ops_chat(body: OpsAgentRequest) -> OpsAgentResponse:
        try:
            return ops.chat(body)
        except LlmUnavailableError as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc

    return app


app = create_app()
