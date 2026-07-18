"""Pipeline Guide tab — design pipelines and import to platform."""

from __future__ import annotations

import json

import streamlit as st

from app.agents.pipeline_guide import PipelineGuideService
from app.platform.bundle import proposed_to_bundle
from app.platform.settings import PlatformSettings
from app.schemas import PipelineGuideRequest, ProposedPipeline
from components.settings import llm_kwargs
from dashpipe_mcp.platform_ops import PlatformOps


def render(platform: PlatformSettings) -> None:
    st.subheader("Pipeline Guide")
    st.caption("Describe a pipeline in natural language. Iterate with follow-up messages.")

    if "guide_messages" not in st.session_state:
        st.session_state.guide_messages = []
    if "current_steps" not in st.session_state:
        st.session_state.current_steps = []
    if "proposed_pipeline" not in st.session_state:
        st.session_state.proposed_pipeline = None

    for role, text in st.session_state.guide_messages:
        with st.chat_message(role):
            st.markdown(text)

    prompt = st.chat_input("Describe your pipeline…")
    if prompt:
        st.session_state.guide_messages.append(("user", prompt))
        svc = PipelineGuideService()
        req = PipelineGuideRequest(
            message=prompt,
            current_steps=st.session_state.current_steps,
            **llm_kwargs(),
        )
        with st.spinner("Designing pipeline…"):
            resp = svc.guide(req, platform_settings=platform)
        st.session_state.guide_messages.append(("assistant", resp.reply))
        if resp.proposed_pipeline:
            st.session_state.proposed_pipeline = resp.proposed_pipeline.model_dump()
            st.session_state.current_steps = [
                {
                    "pipelet_id": s.pipelet_id,
                    "step_order": s.step_order,
                    "execution_config": s.execution_config,
                    "deployment_config": s.deployment_config,
                }
                for s in resp.proposed_pipeline.steps
            ]
        st.rerun()

    proposed_data = st.session_state.proposed_pipeline
    if not proposed_data:
        return

    st.divider()
    st.markdown("**Proposed pipeline**")
    st.json(proposed_data)
    steps = proposed_data.get("steps") or []
    if steps:
        st.dataframe(steps, use_container_width=True)

    proposed = ProposedPipeline.model_validate(proposed_data)
    bundle = proposed_to_bundle(proposed)
    ops = PlatformOps(platform.to_mcp_settings())

    col1, col2, col3 = st.columns(3)
    with col1:
        if st.button("Import to platform", type="primary"):
            try:
                result = ops.import_pipeline(bundle)
                pipeline_id = result.get("id") or result.get("pipeline_id")
                st.success(f"Imported pipeline id: {pipeline_id}")
                st.session_state["last_pipeline_id"] = pipeline_id
            except Exception as exc:  # noqa: BLE001
                st.error(str(exc))
    with col2:
        pid = st.session_state.get("last_pipeline_id")
        if st.button("Dry-run", disabled=not pid):
            try:
                st.json(ops.dry_run_pipeline(pid))
            except Exception as exc:  # noqa: BLE001
                st.error(str(exc))
    with col3:
        pid = st.session_state.get("last_pipeline_id")
        if st.button("Run", disabled=not pid):
            try:
                ops.activate_pipeline(pid)
                st.json(ops.run_pipeline(pid))
            except Exception as exc:  # noqa: BLE001
                st.error(str(exc))

    st.download_button(
        "Download import bundle",
        data=json.dumps(bundle, indent=2),
        file_name=f"{proposed.name.replace(' ', '-').lower()}.pipeline.json",
        mime="application/json",
    )
    ops.close()
