"""Model catalog for Dataflow development on 16 GB Mac."""

from __future__ import annotations

from app.config import settings
from app.llm.providers import LlmProvider, list_ollama_model_names
from app.schemas import AgentModelOption, AgentModelsResponse

NOTE = (
    "AI Dataflow Developer — use Ollama locally for pipelines and pipelets. "
    "Prefer :1b / :1.5b on CPU; :7b often times out."
)

CODE_MODELS = (
    ("qwen2.5-coder:1.5b", 1.2, True, "Best for pipelet Python + K8s on 16 GB Mac."),
    ("llama3.2:1b", 0.9, False, "Fast pipeline scaffolding."),
    ("qwen2.5-coder:3b", 2.2, False, "Higher code quality when memory allows."),
)


def ollama_size_rank(name: str) -> int:
    n = name.lower()
    if "0.5b" in n:
        return 0
    if ":1b" in n or n.endswith("1b"):
        return 1
    if "1.5b" in n:
        return 2
    if "3b" in n or ":latest" in n:
        return 5
    if "7b" in n or "8b" in n or "codellama" in n:
        return 8
    return 4


def ollama_hint(name: str) -> str | None:
    rank = ollama_size_rank(name)
    if rank >= 8:
        return "Very slow on CPU — use :1b or a cloud provider"
    if rank >= 5:
        return "Slow on CPU — prefer llama3.2:1b"
    return None


def list_models() -> AgentModelsResponse:
    models: list[AgentModelOption] = []
    names = sorted(list_ollama_model_names(), key=lambda n: (ollama_size_rank(n), n.lower()))
    if not names:
        models.append(
            AgentModelOption(
                provider="ollama",
                id=settings.chat_model,
                label=f"Ollama / {settings.chat_model}",
                configured=True,
                hint="Start Ollama: docker compose up -d ollama",
            )
        )
    else:
        for name in names:
            models.append(
                AgentModelOption(
                    provider="ollama",
                    id=name,
                    label=f"Ollama / {name}",
                    configured=True,
                    hint=ollama_hint(name),
                )
            )

    if settings.openai_enabled:
        models.append(
            AgentModelOption(
                provider="openai",
                id=settings.openai_default_model,
                label=f"OpenAI / {settings.openai_default_model}",
                configured=bool(settings.openai_api_key),
                hint="Set OPENAI_API_KEY or session api_key" if not settings.openai_api_key else None,
            )
        )
    if settings.anthropic_enabled:
        models.append(
            AgentModelOption(
                provider="anthropic",
                id=settings.anthropic_default_model,
                label=f"Anthropic / {settings.anthropic_default_model}",
                configured=bool(settings.anthropic_api_key),
                hint="Set ANTHROPIC_API_KEY or session api_key" if not settings.anthropic_api_key else None,
            )
        )

    return AgentModelsResponse(
        default_provider=LlmProvider.OLLAMA.value,
        default_model=settings.chat_model,
        models=models,
        note=NOTE,
    )


def code_model_catalog_text() -> str:
    lines = ["Recommended Ollama models for Dataflow pipelet development (16 GB Mac):", ""]
    for name, ram, default, rationale in CODE_MODELS:
        tag = " (default)" if default else ""
        lines += [f"- {name}{tag}", f"  RAM ~{ram} GB", f"  {rationale}", f"  Install: ollama pull {name}", ""]
    return "\n".join(lines).strip()


def code_model_justification(model: str) -> str:
    for name, ram, _, rationale in CODE_MODELS:
        if name == model:
            return f"Selected {name} (~{ram} GB RAM). {rationale}"
    return f"Using {model}. Ensure it fits available RAM on your 16 GB Mac."


def resolve_code_model(requested: str | None, installed: list[str]) -> str:
    if requested and requested.strip():
        return requested.strip()
    ranked = sorted(installed, key=lambda n: (ollama_size_rank(n), n))
    for name, _, _, _ in CODE_MODELS:
        if name in ranked:
            return name
    return ranked[0] if ranked else settings.code_model
