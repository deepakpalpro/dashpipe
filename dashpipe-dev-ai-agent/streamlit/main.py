"""Dashpipe Dev Agent — Streamlit UI."""

from __future__ import annotations

import streamlit as st

from components.settings import render_sidebar
from views import pipeline_guide, pipelet_developer, platform_ops

st.set_page_config(page_title="Dashpipe Dev Agent", layout="wide")
st.title("Dashpipe Dev Agent")
st.caption("Pipeline Guide · Pipelet Developer · Platform Ops (in-process, no MCP subprocess)")

platform = render_sidebar()

guide_tab, dev_tab, ops_tab = st.tabs(["Pipeline Guide", "Pipelet Developer", "Platform Ops"])

with guide_tab:
    pipeline_guide.render(platform)

with dev_tab:
    pipelet_developer.render(platform)

with ops_tab:
    platform_ops.render(platform)
