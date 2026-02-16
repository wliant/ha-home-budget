from langgraph.graph.state import CompiledStateGraph

from ocr_processor.agents.paid import build_graph as build_paid_graph
from ocr_processor.agents.self_hosted import build_graph as build_self_hosted_graph

_AGENTS = {
    "self-hosted": build_self_hosted_graph,
    "paid": build_paid_graph,
}

_compiled_agents: dict[str, CompiledStateGraph] = {}


def get_agent(name: str) -> CompiledStateGraph:
    """Return a compiled agent graph by name. Caches compiled graphs."""
    if name not in _AGENTS:
        valid = ", ".join(sorted(_AGENTS.keys()))
        raise ValueError(f"Unknown agent name: '{name}'. Valid agents: {valid}")

    if name not in _compiled_agents:
        _compiled_agents[name] = _AGENTS[name]().compile()

    return _compiled_agents[name]
