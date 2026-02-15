from typing import TypedDict

from langgraph.graph import END, StateGraph

from ocr_processor.models import CategoryInput, ErrorResponse, ExpenseOutput, ProcessResponse


class AgentState(TypedDict, total=False):
    file_bytes: bytes
    file_type: str
    categories: list[CategoryInput]
    image_bytes: bytes
    extracted_text: str
    line_items: list[dict]
    receipt_date: str | None
    expenses: list[ExpenseOutput]
    error: ErrorResponse | None


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
    graph.add_edge("extract", "classify")
    graph.add_edge("classify", "format")
    graph.add_edge("format", END)

    return graph


_compiled_graph = None


def get_graph():
    global _compiled_graph
    if _compiled_graph is None:
        _compiled_graph = build_graph().compile()
    return _compiled_graph


async def process_receipt(
    file_bytes: bytes,
    content_type: str,
    categories: list[CategoryInput],
) -> ProcessResponse:
    graph = get_graph()

    initial_state: AgentState = {
        "file_bytes": file_bytes,
        "file_type": content_type,
        "categories": categories,
    }

    result = await graph.ainvoke(initial_state)

    return ProcessResponse(expenses=result["expenses"])
