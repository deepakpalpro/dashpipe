from app.agents.knowledge_base import KnowledgeBase
from app.agents.model_catalog import ollama_size_rank, resolve_code_model
from app.agents.pipeline_guide import PipelineGuideService
from app.agents.pipeline_parser import extract_json, kb_fallback


def test_knowledge_base_loads_catalog():
    kb = KnowledgeBase()
    assert len(kb.catalog_core) > 0
    assert "plet-manual-source" in kb.known_pipelet_ids()


def test_extract_json_from_fenced():
    raw = 'Here:\n```json\n{"reply": "hi", "thinking": "t"}\n```'
    data = extract_json(raw)
    assert data is not None
    assert data["reply"] == "hi"


def test_fallback_manual_s3():
    kb = KnowledgeBase()
    resp = kb_fallback(kb, "manual trigger to s3 run")
    assert resp is not None
    assert resp.proposed_pipeline is not None
    assert len(resp.proposed_pipeline.steps) >= 2


def test_pipeline_guide_service_fallback():
    svc = PipelineGuideService()
    resp = svc._fallback("manual trigger to s3 run")
    assert resp is not None
    assert resp.proposed_pipeline is not None


def test_resolve_code_model_prefers_qwen():
    assert resolve_code_model(None, ["llama3.2:1b", "qwen2.5-coder:1.5b"]) == "qwen2.5-coder:1.5b"


def test_ollama_rank():
    assert ollama_size_rank("llama3.2:1b") < ollama_size_rank("codellama:7b")
