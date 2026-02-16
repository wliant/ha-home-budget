from typing import TypedDict

from ocr_processor.config import settings
from ocr_processor.models import (
    CategoryInput,
    ClassifiedLineItem,
    ErrorResponse,
    ExpenseOutput,
    ExtractedLineItem,
    ProcessResponse,
)


class AgentState(TypedDict, total=False):
    file_bytes: bytes
    file_type: str
    categories: list[CategoryInput]
    selected_category: CategoryInput | None
    image_bytes: bytes
    extracted_text: str
    line_items: list[ExtractedLineItem]
    receipt_date: str | None
    total_amount: str | None
    classified_items: list[ClassifiedLineItem]
    expenses: list[ExpenseOutput]
    error: ErrorResponse | None


def _route_after_extract(state: AgentState) -> str:
    """Route to format if selected_category is present, otherwise to classify."""
    if state.get("selected_category") is not None:
        return "format"
    return "classify"


async def process_receipt(
    file_bytes: bytes,
    content_type: str,
    categories: list[CategoryInput],
    selected_category: CategoryInput | None = None,
) -> ProcessResponse:
    from ocr_processor.agents import get_agent
    from ocr_processor.callbacks import get_ocr_processing_logger

    graph = get_agent(settings.agent_name)

    initial_state: AgentState = {
        "file_bytes": file_bytes,
        "file_type": content_type,
        "categories": categories,
    }

    if selected_category is not None:
        initial_state["selected_category"] = selected_category

    config = {"callbacks": [get_ocr_processing_logger()]}

    result = await graph.ainvoke(initial_state, config=config)

    return ProcessResponse(expenses=result["expenses"])
