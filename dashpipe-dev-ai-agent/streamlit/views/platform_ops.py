"""Platform Ops tab — pipelines, executions, connectors, catalog, ops chat."""

from __future__ import annotations

import json

import streamlit as st

from app.agents.ops_agent import OpsAgentService
from app.platform.settings import PlatformSettings
from app.schemas import OpsAgentRequest
from components.settings import llm_kwargs
from dashpipe_mcp.platform_ops import PlatformOps


def render(platform: PlatformSettings) -> None:
    st.subheader("Platform Ops")
    ops = PlatformOps(platform.to_mcp_settings())

    tab_status, tab_pipes, tab_exec, tab_conn, tab_catalog, tab_chat = st.tabs(
        ["Status", "Pipelines", "Executions", "Connectors", "Catalog", "Ops chat"]
    )

    with tab_status:
        _render_status(ops, platform)

    with tab_pipes:
        _render_pipelines(ops)

    with tab_exec:
        _render_executions(ops)

    with tab_conn:
        _render_connectors(ops)

    with tab_catalog:
        _render_catalog(ops)

    with tab_chat:
        _render_ops_chat(platform)

    ops.close()


def _render_status(ops: PlatformOps, platform: PlatformSettings) -> None:
    try:
        health = ops.dashpipe_health()
        st.success("Dashpipe API is reachable")
        st.json(health)
        links = ops.get_observability_links()
        if links:
            st.markdown("**Observability**")
            st.json(links)
    except Exception as exc:  # noqa: BLE001
        st.error(f"API unreachable at {platform.api_url}: {exc}")
    st.caption(f"Tenant: `{platform.tenant_id}`")


def _render_pipelines(ops: PlatformOps) -> None:
    try:
        pipelines = ops.list_pipelines()
    except Exception as exc:  # noqa: BLE001
        st.error(str(exc))
        return

    if not pipelines:
        st.info("No pipelines yet.")
    else:
        labels = [
            f"{p.get('name', 'unnamed')} ({p.get('id')}) — {p.get('status', '?')}"
            for p in pipelines
        ]
        idx = st.selectbox("Select pipeline", range(len(pipelines)), format_func=lambda i: labels[i])
        selected = pipelines[idx]
        pipeline_id = selected.get("id")
        st.session_state["selected_pipeline_id"] = pipeline_id
        st.json(selected)

        c1, c2, c3, c4 = st.columns(4)
        with c1:
            if st.button("Activate"):
                st.json(ops.activate_pipeline(pipeline_id))
        with c2:
            if st.button("Deactivate"):
                st.json(ops.deactivate_pipeline(pipeline_id))
        with c3:
            if st.button("Dry-run"):
                st.json(ops.dry_run_pipeline(pipeline_id))
        with c4:
            if st.button("Run"):
                st.json(ops.run_pipeline(pipeline_id))

        bundle = ops.export_pipeline(pipeline_id)
        st.download_button(
            "Export JSON",
            data=json.dumps(bundle, indent=2),
            file_name=f"{selected.get('name', 'pipeline')}.pipeline.json",
            mime="application/json",
        )

    with st.expander("Create pipeline"):
        name = st.text_input("Name", key="new_pipe_name")
        description = st.text_input("Description", key="new_pipe_desc")
        if st.button("Create", disabled=not name.strip()):
            st.json(ops.create_pipeline(name.strip(), description))


def _render_executions(ops: PlatformOps) -> None:
    pipeline_id = st.text_input(
        "Pipeline ID",
        value=st.session_state.get("selected_pipeline_id") or st.session_state.get("last_pipeline_id") or "",
    )
    if not pipeline_id:
        st.info("Select or enter a pipeline id.")
        return
    try:
        executions = ops.list_executions(pipeline_id)
    except Exception as exc:  # noqa: BLE001
        st.error(str(exc))
        return
    if not executions:
        st.info("No executions.")
        return
    for ex in executions[:20]:
        eid = ex.get("id") or ex.get("execution_id")
        with st.expander(f"{eid} — {ex.get('status', '?')}"):
            st.json(ex)
            c1, c2 = st.columns(2)
            with c1:
                if st.button("Cancel", key=f"cancel-{eid}"):
                    st.json(ops.cancel_execution(pipeline_id, eid))
            with c2:
                if st.button("Debug", key=f"debug-{eid}"):
                    st.json(ops.debug_execution(pipeline_id, eid))
            if st.button("Show logs", key=f"logs-{eid}"):
                st.json(ops.get_execution_logs(eid))


def _render_connectors(ops: PlatformOps) -> None:
    try:
        connectors = ops.list_connectors()
        types = ops.list_connector_types()
    except Exception as exc:  # noqa: BLE001
        st.error(str(exc))
        return

    st.markdown("**Connector types**")
    st.dataframe(types if isinstance(types, list) else [types], use_container_width=True)
    st.markdown("**Tenant connectors**")
    st.dataframe(connectors if isinstance(connectors, list) else [connectors], use_container_width=True)

    with st.expander("Create connector"):
        type_id = st.text_input("Connector type id", value="ct-rest")
        name = st.text_input("Name", value="My REST connector")
        config_raw = st.text_area("Config JSON", value="{}")
        if st.button("Create connector", disabled=not name.strip()):
            try:
                config = json.loads(config_raw or "{}")
                st.json(ops.create_connector(type_id, name.strip(), config=config))
            except json.JSONDecodeError as exc:
                st.error(f"Invalid JSON: {exc}")

    test_id = st.text_input("Connector id to test")
    if st.button("Test connector", disabled=not test_id.strip()):
        st.json(ops.test_connector(test_id.strip()))


def _render_catalog(ops: PlatformOps) -> None:
    query = st.text_input("Search pipelets")
    active_only = st.checkbox("Active only", value=True)
    try:
        pipelets = ops.list_pipelets(query=query or None, active_only=active_only, limit=100)
        st.dataframe(pipelets, use_container_width=True)
    except Exception as exc:  # noqa: BLE001
        st.error(str(exc))


def _render_ops_chat(platform: PlatformSettings) -> None:
    if "ops_messages" not in st.session_state:
        st.session_state.ops_messages = []

    for role, text in st.session_state.ops_messages:
        with st.chat_message(role):
            st.markdown(text)

    prompt = st.chat_input("Ask platform ops…")
    if prompt:
        st.session_state.ops_messages.append(("user", prompt))
        svc = OpsAgentService()
        req = OpsAgentRequest(
            message=prompt,
            pipeline_id=st.session_state.get("selected_pipeline_id"),
            **llm_kwargs(),
        )
        with st.spinner("Running ops agent…"):
            resp = svc.chat(req, platform_settings=platform)
        st.session_state.ops_messages.append(("assistant", resp.reply))
        if resp.tool_calls:
            with st.expander("Tool trace"):
                st.json([t.model_dump() for t in resp.tool_calls])
        st.rerun()
