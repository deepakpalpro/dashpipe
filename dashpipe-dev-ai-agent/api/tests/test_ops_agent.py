from unittest.mock import MagicMock, patch

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage
from langchain_core.outputs import ChatGeneration, ChatResult

from app.graphs.ops_graph import run_ops_agent
from app.platform.tools import build_platform_tools
from app.schemas import OpsAgentRequest


class FakeChatModel(BaseChatModel):
    def _generate(self, messages, stop=None, run_manager=None, **kwargs):
        return ChatResult(generations=[ChatGeneration(message=AIMessage(content="Ops reply."))])

    @property
    def _llm_type(self) -> str:
        return "fake"


def test_build_platform_tools_count():
    tools = build_platform_tools()
    assert len(tools) >= 28
    names = {t.name for t in tools}
    assert "list_pipelines" in names
    assert "debug_execution" in names


def test_ops_agent_with_mock_graph():
    request = OpsAgentRequest(message="List pipelines", provider="ollama", model="fake")
    fake_model = FakeChatModel()

    mock_agent = MagicMock()
    mock_agent.invoke.return_value = {
        "messages": [AIMessage(content="Found 0 pipelines.", tool_calls=[])]
    }

    with patch("app.graphs.ops_graph.create_react_agent", return_value=mock_agent):
        resp = run_ops_agent(request, build_platform_tools(), model=fake_model)

    assert "pipeline" in resp.reply.lower() or resp.reply
    mock_agent.invoke.assert_called_once()
