from datetime import date
from decimal import Decimal

from pydantic import BaseModel, Field


class CategoryInput(BaseModel):
    id: int
    name: str


# LLM Structured Output Models

class ExtractedLineItem(BaseModel):
    description: str
    amount: Decimal


class ReceiptExtraction(BaseModel):
    is_receipt: bool
    date: str | None = None
    total: Decimal
    line_items: list[ExtractedLineItem]


class ClassifiedLineItem(BaseModel):
    description: str
    amount: Decimal
    category_id: int | None = None
    category_name: str | None = None


class ClassificationResult(BaseModel):
    items: list[ClassifiedLineItem]


# Output Models

class ExpenseOutput(BaseModel):
    amount: Decimal = Field(decimal_places=2)
    description: str
    expense_date: date
    category_id: int | None = None
    category_name: str | None = None


class ProcessResponse(BaseModel):
    expenses: list[ExpenseOutput] = Field(min_length=1)


class ErrorResponse(BaseModel):
    error_code: str
    message: str
    retryable: bool
