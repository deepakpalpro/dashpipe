"""Runtime configuration for the Dataflow developer agent."""

from __future__ import annotations

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    enabled: bool = True
    ollama_base_url: str = "http://localhost:11434"
    chat_model: str = "llama3.2:1b"
    code_model: str = "qwen2.5-coder:1.5b"
    connect_timeout_s: float = 5.0
    read_timeout_s: float = 180.0

    openai_enabled: bool = True
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_default_model: str = "gpt-4o-mini"

    anthropic_enabled: bool = True
    anthropic_api_key: str = ""
    anthropic_base_url: str = "https://api.anthropic.com"
    anthropic_default_model: str = "claude-3-5-haiku-latest"


settings = Settings()
