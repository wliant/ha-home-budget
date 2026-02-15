from datetime import date
from decimal import Decimal
from unittest.mock import patch

import pytest

from ocr_processor.nodes.format import format_node


@pytest.mark.asyncio
async def test_amounts_formatted_to_two_decimal_places():
    """Amounts should be formatted as Decimal with 2 decimal places."""
    state = {
        "line_items": [
            {"description": "Coffee", "amount": 4.5, "category_id": 1, "category_name": "Dining"},
        ],
        "receipt_date": "2026-02-15",
    }
    result = await format_node(state)
    expenses = result["expenses"]
    assert len(expenses) == 1
    assert expenses[0].amount == Decimal("4.50")


@pytest.mark.asyncio
async def test_date_parsed_from_iso_format():
    """Receipt date should be parsed from ISO format string."""
    state = {
        "line_items": [
            {"description": "Milk", "amount": 3.99, "category_id": 1, "category_name": "Groceries"},
        ],
        "receipt_date": "2026-01-20",
    }
    result = await format_node(state)
    expenses = result["expenses"]
    assert expenses[0].expense_date == date(2026, 1, 20)


@pytest.mark.asyncio
async def test_missing_receipt_date_defaults_to_today():
    """Missing receipt_date should default to today."""
    state = {
        "line_items": [
            {"description": "Item", "amount": 10.00, "category_id": 1, "category_name": "Groceries"},
        ],
        "receipt_date": None,
    }
    result = await format_node(state)
    expenses = result["expenses"]
    assert expenses[0].expense_date == date.today()


@pytest.mark.asyncio
async def test_category_fields_preserved():
    """Category ID and name should be preserved from input."""
    state = {
        "line_items": [
            {"description": "Bus fare", "amount": 2.50, "category_id": 3, "category_name": "Transport"},
        ],
        "receipt_date": "2026-02-15",
    }
    result = await format_node(state)
    expenses = result["expenses"]
    assert expenses[0].category_id == 3
    assert expenses[0].category_name == "Transport"


@pytest.mark.asyncio
async def test_multiple_items_formatted():
    """Multiple items should all be formatted correctly."""
    state = {
        "line_items": [
            {"description": "Milk", "amount": 3.99, "category_id": 1, "category_name": "Groceries"},
            {"description": "Coffee", "amount": 4.50, "category_id": 2, "category_name": "Dining"},
            {"description": "Bus", "amount": 2.75, "category_id": 3, "category_name": "Transport"},
        ],
        "receipt_date": "2026-02-15",
    }
    result = await format_node(state)
    expenses = result["expenses"]
    assert len(expenses) == 3
    assert expenses[0].description == "Milk"
    assert expenses[1].description == "Coffee"
    assert expenses[2].description == "Bus"
    assert expenses[0].amount == Decimal("3.99")
    assert expenses[1].amount == Decimal("4.50")
    assert expenses[2].amount == Decimal("2.75")


@pytest.mark.asyncio
async def test_amount_rounding():
    """Amounts with more than 2 decimal places should be rounded properly."""
    state = {
        "line_items": [
            {"description": "Item", "amount": 1.555, "category_id": 1, "category_name": "Groceries"},
        ],
        "receipt_date": "2026-02-15",
    }
    result = await format_node(state)
    expenses = result["expenses"]
    # ROUND_HALF_UP: 1.555 -> 1.56
    assert expenses[0].amount == Decimal("1.56")
