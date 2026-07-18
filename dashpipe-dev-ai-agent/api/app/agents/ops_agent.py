"""Platform ops conversational agent."""

from __future__ import annotations

from app.graphs.ops_graph import run_ops_agent
from app.platform.settings import PlatformSettings
from app.platform.tools import build_platform_tools
from app.schemas import OpsAgentRequest, OpsAgentResponse


class OpsAgentService:
    def chat(
        self,
        request: OpsAgentRequest,
        platform_settings: PlatformSettings | None = None,
    ) -> OpsAgentResponse:
        tools = build_platform_tools(platform_settings)
        return run_ops_agent(request, tools)
