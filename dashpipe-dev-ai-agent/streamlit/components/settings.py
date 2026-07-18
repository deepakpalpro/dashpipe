"""Sidebar settings shared across Streamlit views."""

from __future__ import annotations

import os

import streamlit as st

from app.platform.settings import PlatformSettings


def render_sidebar() -> PlatformSettings:
    st.sidebar.header("Connection")
    api_url = st.sidebar.text_input(
        "Dashpipe API URL",
        value=os.environ.get("DASHPIPE_API_URL", "http://localhost:8080"),
    )
    tenant_id = st.sidebar.text_input(
        "Tenant ID",
        value=os.environ.get("DASHPIPE_TENANT_ID", "T001"),
    )

    st.sidebar.header("LLM")
    provider = st.sidebar.selectbox(
        "Provider",
        options=["ollama", "openai", "anthropic"],
        index=0,
    )
    model = st.sidebar.text_input("Model", value=os.environ.get("CHAT_MODEL", "llama3.2:1b"))
    api_key = st.sidebar.text_input("API key (cloud providers)", type="password", value="")

    st.session_state["llm_provider"] = provider
    st.session_state["llm_model"] = model
    st.session_state["llm_api_key"] = api_key or None

    return PlatformSettings(api_url=api_url.strip(), tenant_id=tenant_id.strip())


def llm_kwargs() -> dict[str, str | None]:
    return {
        "provider": st.session_state.get("llm_provider", "ollama"),
        "model": st.session_state.get("llm_model"),
        "api_key": st.session_state.get("llm_api_key"),
    }
