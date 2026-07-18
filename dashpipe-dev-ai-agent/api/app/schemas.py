"""API request/response models."""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class CatalogPipeletSummary(BaseModel):
    id: str
    name: str | None = None
    category: str | None = None
    description: str | None = None
    required_execution_keys: list[str] = Field(default_factory=list)


class PipelineGuideRequest(BaseModel):
    message: str = Field(..., min_length=1)
    provider: str | None = None
    model: str | None = None
    api_key: str | None = None
    catalog: list[CatalogPipeletSummary] = Field(default_factory=list)
    current_steps: list[dict[str, Any]] = Field(default_factory=list)


class ProposedStep(BaseModel):
    pipelet_id: str
    step_order: int
    execution_config: dict[str, Any] = Field(default_factory=dict)
    deployment_config: dict[str, Any] = Field(default_factory=dict)
    connector_ids: list[str] = Field(default_factory=list)
    service_ids: list[str] = Field(default_factory=list)


class ProposedPipeline(BaseModel):
    name: str
    description: str = ""
    steps: list[ProposedStep] = Field(default_factory=list)


class PipelineGuideResponse(BaseModel):
    reply: str
    thinking: str | None = None
    proposed_pipeline: ProposedPipeline | None = None
    model: str | None = None
    raw_model_output: str | None = None


class PythonDeveloperRequest(BaseModel):
    requirement: str = Field(..., min_length=1)
    provider: str | None = None
    model: str | None = None
    context: str | None = None
    api_key: str | None = None


class GeneratedFile(BaseModel):
    path: str
    content: str


class PythonDeveloperResponse(BaseModel):
    model: str
    model_justification: str
    thinking: str
    plain_text: str
    files: list[GeneratedFile] = Field(default_factory=list)
    raw_model_output: str | None = None


class AgentModelOption(BaseModel):
    provider: str
    id: str
    label: str
    configured: bool = True
    hint: str | None = None


class AgentModelsResponse(BaseModel):
    default_provider: str
    default_model: str
    models: list[AgentModelOption]
    note: str | None = None


class OpsToolCallLog(BaseModel):
    tool: str
    args: dict[str, Any] = Field(default_factory=dict)
    result: str | None = None


class OpsAgentRequest(BaseModel):
    message: str = Field(..., min_length=1)
    provider: str | None = None
    model: str | None = None
    api_key: str | None = None
    pipeline_id: str | None = None
    execution_id: str | None = None
    proposed_pipeline: dict[str, Any] | None = None


class OpsAgentResponse(BaseModel):
    reply: str
    tool_calls: list[OpsToolCallLog] = Field(default_factory=list)
    pipeline_id: str | None = None
    execution_id: str | None = None
    model: str | None = None


class PlatformStatusResponse(BaseModel):
    healthy: bool
    api_url: str
    tenant_id: str
    health: dict[str, Any] = Field(default_factory=dict)
    config: dict[str, Any] = Field(default_factory=dict)
    error: str | None = None
