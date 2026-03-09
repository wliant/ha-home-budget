/**
 * Shared expense form validation logic.
 * Used by RecordExpenseDialog and NewExpensePage.
 */

export interface ExpenseValidationInput {
  amount: string;
  description: string;
  expenseDate: string;
  categoryId: number | null;
  filesCount: number;
  maxFiles: number;
}

export function validateExpenseForm(input: ExpenseValidationInput): Record<string, string> {
  const errors: Record<string, string> = {};

  const amountNum = parseFloat(input.amount);
  if (!input.amount || isNaN(amountNum) || amountNum === 0) {
    errors.amount = 'Amount must not be zero';
  }

  if (!input.description.trim()) {
    errors.description = 'Description is required';
  } else if (input.description.length > 500) {
    errors.description = 'Description must be 500 characters or less';
  }

  if (!input.categoryId) {
    errors.category = 'Category is required';
  }

  if (!input.expenseDate) {
    errors.date = 'Date is required';
  }

  if (input.filesCount > input.maxFiles) {
    errors.files = `Maximum ${input.maxFiles} files allowed`;
  }

  return errors;
}
