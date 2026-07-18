"""Pipelet Developer tab — generate Python/K8s artifacts."""

from __future__ import annotations

import streamlit as st

from app.agents.python_developer import PythonDeveloperService
from app.platform.settings import PlatformSettings
from app.schemas import PythonDeveloperRequest
from components.settings import llm_kwargs


def render(_platform: PlatformSettings) -> None:
    st.subheader("Pipelet Developer")
    st.caption("Describe a pipelet requirement to generate Python and deployment files.")

    requirement = st.text_area("Requirement", height=120, placeholder="REST source pipelet that polls…")
    context = st.text_area("Optional context", height=80)

    if st.button("Generate", type="primary", disabled=not requirement.strip()):
        svc = PythonDeveloperService()
        req = PythonDeveloperRequest(
            requirement=requirement.strip(),
            context=context.strip() or None,
            **llm_kwargs(),
        )
        with st.spinner("Generating…"):
            resp = svc.generate(req)

        st.markdown(resp.plain_text)
        if resp.thinking:
            with st.expander("Thinking"):
                st.markdown(resp.thinking)
        for file in resp.files:
            with st.expander(file.path):
                st.code(file.content, language=_guess_lang(file.path))
                st.download_button(
                    f"Download {file.path}",
                    data=file.content,
                    file_name=file.path.split("/")[-1],
                )


def _guess_lang(path: str) -> str:
    if path.endswith(".py"):
        return "python"
    if path.endswith((".yaml", ".yml")):
        return "yaml"
    if path.endswith(".json"):
        return "json"
    return "text"
