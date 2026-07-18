"""LangChain chat model factory and Ollama model discovery."""

from __future__ import annotations

from enum import Enum

import httpx
from langchain_anthropic import ChatAnthropic
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, BaseMessage
from langchain_ollama import ChatOllama
from langchain_openai import ChatOpenAI

from app.config import settings
from app.llm.exceptions import LlmUnavailableError


class LlmProvider(str, Enum):
    OLLAMA = "ollama"
    OPENAI = "openai"
    ANTHROPIC = "anthropic"

    @classmethod
    def from_wire(cls, raw: str | None) -> LlmProvider:
        if raw is None or not raw.strip():
            return cls.OLLAMA
        try:
            return cls(raw.strip().lower())
        except ValueError as exc:
            raise ValueError(f"Unknown provider: {raw}") from exc


def default_model(provider: LlmProvider) -> str:
    if provider == LlmProvider.OPENAI:
        return settings.openai_default_model
    if provider == LlmProvider.ANTHROPIC:
        return settings.anthropic_default_model
    return settings.chat_model


def list_ollama_model_names() -> list[str]:
    base = settings.ollama_base_url.rstrip("/")
    try:
        response = httpx.get(
            f"{base}/api/tags",
            timeout=settings.connect_timeout_s,
        )
        response.raise_for_status()
    except httpx.HTTPError:
        return []
    models = response.json().get("models") or []
    return [
        item["name"].strip()
        for item in models
        if isinstance(item.get("name"), str) and item["name"].strip()
    ]


def get_chat_model(
    provider: LlmProvider,
    model: str,
    *,
    api_key: str | None = None,
    code_mode: bool = False,
) -> BaseChatModel:
    if provider == LlmProvider.OPENAI:
        if not settings.openai_enabled:
            raise LlmUnavailableError("OpenAI provider is disabled")
        key = (api_key or settings.openai_api_key or "").strip()
        if not key:
            raise LlmUnavailableError(
                "OpenAI API key missing. Set OPENAI_API_KEY or pass api_key."
            )
        return ChatOpenAI(
            model=model or settings.openai_default_model,
            api_key=key,
            base_url=settings.openai_base_url,
            temperature=0.15 if code_mode else 0.2,
            max_tokens=4096 if code_mode else 800,
            timeout=settings.read_timeout_s,
        )

    if provider == LlmProvider.ANTHROPIC:
        if not settings.anthropic_enabled:
            raise LlmUnavailableError("Anthropic provider is disabled")
        key = (api_key or settings.anthropic_api_key or "").strip()
        if not key:
            raise LlmUnavailableError(
                "Anthropic API key missing. Set ANTHROPIC_API_KEY or pass api_key."
            )
        return ChatAnthropic(
            model=model or settings.anthropic_default_model,
            api_key=key,
            base_url=settings.anthropic_base_url,
            temperature=0.15 if code_mode else 0.2,
            max_tokens=4096 if code_mode else 800,
            timeout=settings.read_timeout_s,
        )

    return ChatOllama(
        model=model or (settings.code_model if code_mode else settings.chat_model),
        base_url=settings.ollama_base_url,
        num_predict=4096 if code_mode else 280,
        num_ctx=8192 if code_mode else 2048,
        temperature=0.15 if code_mode else 0.2,
        keep_alive="10m",
        client_kwargs={"timeout": settings.read_timeout_s},
    )


def invoke_chat(
    provider: LlmProvider,
    *,
    model: str,
    system: str,
    user: str,
    api_key: str | None = None,
    code_mode: bool = False,
) -> str:
    from langchain_core.messages import HumanMessage, SystemMessage

    chat_model = get_chat_model(
        provider, model, api_key=api_key, code_mode=code_mode
    )
    messages: list[BaseMessage] = [
        SystemMessage(content=system),
        HumanMessage(content=user),
    ]
    try:
        result: AIMessage = chat_model.invoke(messages)  # type: ignore[assignment]
    except Exception as exc:
        msg = str(exc)
        if "timeout" in msg.lower():
            raise LlmUnavailableError(
                f"LLM timed out for {provider.value}/{model}. Try a smaller Ollama model."
            ) from exc
        raise LlmUnavailableError(
            f"{provider.value} unavailable: {msg}"
        ) from exc

    content = result.content
    if isinstance(content, list):
        content = "".join(
            block.get("text", "") if isinstance(block, dict) else str(block)
            for block in content
        )
    if not isinstance(content, str) or not content.strip():
        raise LlmUnavailableError("LLM returned an empty completion")
    return content.strip()
