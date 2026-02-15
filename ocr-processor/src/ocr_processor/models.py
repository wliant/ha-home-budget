from datetime import date
from decimal import Decimal

from pydantic import BaseModel, Field


class CategoryInput(BaseModel):
    id: int
    name: str


class ExpenseOutput(BaseModel):
    amount: Decimal = Field(decimal_places=2)
    description: str
    expense_date: date
    category_id: int
    category_name: str


class ProcessResponse(BaseModel):
    expenses: list[ExpenseOutput] = Field(min_length=1)


class ErrorResponse(BaseModel):
    error_code: str
    message: str
    retryable: bool
