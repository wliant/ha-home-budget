from datetime import date
from decimal import ROUND_HALF_UP, Decimal

from ocr_processor.logging import get_logger, log_agent_step
from ocr_processor.models import ClassifiedLineItem, ExpenseOutput

logger = get_logger(__name__)


async def format_node(state: dict) -> dict:
    classified_items = state.get("classified_items", [])
    selected_category = state.get("selected_category")
    receipt_date = state.get("receipt_date") or date.today().isoformat()
    total_amount = state.get("total_amount")

    # If selected_category is present but no classified_items (classification was skipped),
    # build classified_items from extracted line_items with the selected category
    if selected_category is not None and not classified_items:
        line_items = state.get("line_items", [])
        classified_items = [
            ClassifiedLineItem(
                description=item.description,
                amount=item.amount,
                category_id=selected_category.id,
                category_name=selected_category.name,
            )
            for item in line_items
        ]

    log_agent_step(
        logger,
        "format",
        input_summary=f"items={len(classified_items)}, date={receipt_date}, selected_category={selected_category is not None}",
    )

    expense_date = date.fromisoformat(receipt_date)

    # Smart consolidation: if all items share the same non-null category, consolidate into one
    if classified_items and _all_same_category(classified_items):
        first = classified_items[0]
        consolidated_amount = Decimal(str(total_amount)) if total_amount else sum(
            Decimal(str(item.amount)) for item in classified_items
        )
        consolidated_amount = consolidated_amount.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
        joined_description = ", ".join(item.description for item in classified_items)

        expenses = [ExpenseOutput(
            amount=consolidated_amount,
            description=joined_description,
            expense_date=expense_date,
            category_id=first.category_id,
            category_name=first.category_name,
        )]
    else:
        # Different categories or any null — return one expense per item
        expenses = []
        for item in classified_items:
            amount = Decimal(str(item.amount)).quantize(
                Decimal("0.01"), rounding=ROUND_HALF_UP
            )
            expense = ExpenseOutput(
                amount=amount,
                description=item.description,
                expense_date=expense_date,
                category_id=item.category_id,
                category_name=item.category_name,
            )
            expenses.append(expense)

    log_agent_step(
        logger,
        "format",
        output_summary=f"expenses={len(expenses)}, consolidated={len(classified_items) > 0 and len(expenses) == 1}",
    )

    return {"expenses": expenses}


def _all_same_category(items: list[ClassifiedLineItem]) -> bool:
    """Check if all items share the same non-null category."""
    first_id = items[0].category_id
    if first_id is None:
        return False
    return all(item.category_id == first_id for item in items)
