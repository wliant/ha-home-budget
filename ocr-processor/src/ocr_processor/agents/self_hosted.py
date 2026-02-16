from langgraph.graph import END, StateGraph

from ocr_processor.agent import AgentState, _route_after_extract

NAME = "self-hosted"


def build_graph() -> StateGraph:
    from ocr_processor.nodes.classify import classify_node
    from ocr_processor.nodes.extract import extract_node
    from ocr_processor.nodes.format import format_node
    from ocr_processor.nodes.validate import validate_node

    graph = StateGraph(AgentState)

    graph.add_node("validate", validate_node)
    graph.add_node("extract", extract_node)
    graph.add_node("classify", classify_node)
    graph.add_node("format", format_node)

    graph.set_entry_point("validate")
    graph.add_edge("validate", "extract")
    graph.add_conditional_edges("extract", _route_after_extract, {"classify": "classify", "format": "format"})
    graph.add_edge("classify", "format")
    graph.add_edge("format", END)

    return graph
