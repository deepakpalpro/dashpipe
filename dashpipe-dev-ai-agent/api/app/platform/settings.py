"""Platform API connection settings (shared by tools, ops agent, Streamlit)."""

from __future__ import annotations

from pydantic import BaseModel, Field

from app.config import settings as app_settings
from dashpipe_mcp.config import Settings as McpSettings


class PlatformSettings(BaseModel):
    api_url: str = Field(default="http://localhost:8080")
    tenant_id: str = Field(default="T001")
    request_timeout_s: float = Field(default=60.0)

    @classmethod
    def from_env(cls) -> PlatformSettings:
        return cls(
            api_url=getattr(app_settings, "dashpipe_api_url", "http://localhost:8080"),
            tenant_id=getattr(app_settings, "dashpipe_tenant_id", "T001"),
            request_timeout_s=60.0,
        )

    def to_mcp_settings(self) -> McpSettings:
        return McpSettings(
            api_url=self.api_url,
            tenant_id=self.tenant_id,
            request_timeout_s=self.request_timeout_s,
        )
