from datetime import date
from decimal import ROUND_HALF_UP, Decimal

from ocr_processor.logging import get_logger, log_agent_step
from ocr_processor.models import ExpenseOutput

logger = get_logger(__name__)


async def format_node(state: dict) -> dict:
    line_items = state["line_items"]
    receipt_date = state.get("receipt_date") or date.today().isoformat()

    log_agent_step(
        logger,
        "format",
        input_summary=f"items={len(line_items)}, date={receipt_date}",
    )

    expenses = []
    for item in line_items:
        amount = Decimal(str(item["amount"])).quantize(
            Decimal("0.01"), rounding=ROUND_HALF_UP
        )
        expense = ExpenseOutput(
            amount=amount,
            description=item.get("description", "Receipt item"),
            expense_date=date.fromisoformat(receipt_date),
            category_id=item["category_id"],
            category_name=item["category_name"],
        )
        expenses.append(expense)

    log_agent_step(
        logger,
        "format",
        output_summary=f"expenses={len(expenses)}",
    )

    return {"expenses": expenses}
