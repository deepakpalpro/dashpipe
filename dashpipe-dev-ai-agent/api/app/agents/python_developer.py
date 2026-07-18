"""Generate pipelet code via LangChain chat models."""

from __future__ import annotations

import re
from pathlib import Path

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

from app.agents.model_catalog import code_model_justification, resolve_code_model
from app.llm.providers import LlmProvider, default_model, get_chat_model, list_ollama_model_names
from app.schemas import GeneratedFile, PythonDeveloperRequest, PythonDeveloperResponse

_KNOWLEDGE = Path(__file__).resolve().parent.parent / "knowledge"

SYSTEM_PROMPT = """
You are the AI Dataflow Developer — generate Python pipelets and Kubernetes artifacts for the Dataflow platform.

Goals:
1. Python 3.12+ pipelets following SOLID, DRY, KISS.
2. Split logic.py (pure transforms) and main.py (I/O shell using io_transport + config_merge).
3. Include Dockerfile (python:3.12-slim) and optional K8s Job manifest when relevant.
4. Pipelets are batch Jobs: read JSON from stdin/INPUT_QUEUE, write JSON to stdout/OUTPUT_QUEUE.

Output rules (strict):
- Plain text only — no markdown fences.
- Each artifact: # file: relative/path on its own line, then full contents.
- Artifacts: logic.py, main.py, pipelet.json, Dockerfile, *.yaml, requirements.txt.
- Never invent secrets — use placeholders.
- Log to stderr; JSON on stdout for pipelet Jobs.
""".strip()

_CODE_PROMPT = ChatPromptTemplate.from_messages(
    [
        ("system", "{system}"),
        ("user", "{user}"),
    ]
)


class PythonDeveloperService:
    def generate(self, request: PythonDeveloperRequest) -> PythonDeveloperResponse:
        provider = LlmProvider.from_wire(request.provider)
        installed = list_ollama_model_names() if provider == LlmProvider.OLLAMA else []
        model = (
            resolve_code_model(request.model, installed)
            if provider == LlmProvider.OLLAMA
            else (request.model or default_model(provider))
        )

        user = request.requirement.strip()
        if request.context and request.context.strip():
            user = f"Requirement:\n{user}\n\nAdditional context:\n{request.context.strip()}"

        chat_model = get_chat_model(
            provider,
            model,
            api_key=request.api_key,
            code_mode=True,
        )
        chain = _CODE_PROMPT | chat_model | StrOutputParser()
        raw = chain.invoke({"system": _system_prompt(), "user": user})

        files = _parse_files(raw)
        plain = _format_plain(files, raw)
        justification = code_model_justification(model)
        return PythonDeveloperResponse(
            model=f"{provider.value}/{model}",
            model_justification=justification,
            thinking=justification,
            plain_text=plain,
            files=files,
            raw_model_output=raw,
        )


def _system_prompt() -> str:
    parts = [SYSTEM_PROMPT]
    for name in ("python-developer-rules.md", "k8s-python-patterns.md"):
        path = _KNOWLEDGE / name
        if path.is_file():
            parts.append(path.read_text(encoding="utf-8").strip())
    return "\n\n---\n\n".join(parts)


_FILE_HEADER = re.compile(r"^#\s*file:\s*(?P<path>.+?)\s*$", re.MULTILINE)


def _parse_files(raw: str) -> list[GeneratedFile]:
    text = (raw or "").strip()
    matches = list(_FILE_HEADER.finditer(text))
    if not matches:
        return []
    files: list[GeneratedFile] = []
    for i, match in enumerate(matches):
        path = match.group("path").strip()
        start = match.end()
        end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        files.append(GeneratedFile(path=path, content=text[start:end].strip("\n")))
    return files


def _format_plain(files: list[GeneratedFile], raw: str) -> str:
    if not files:
        return raw.strip()
    chunks: list[str] = []
    for f in files:
        chunks += [f"# file: {f.path}", f.content.rstrip(), ""]
    return "\n".join(chunks).strip()
