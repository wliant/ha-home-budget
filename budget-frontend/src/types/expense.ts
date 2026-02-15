/**
 * Shared TypeScript interfaces for expense management
 * Feature 007: Expense Recording
 */

/**
 * Expense form state for managing form inputs and validation
 */
export interface ExpenseFormState {
  amount: string;                    // String for input control, convert to number on submit
  description: string;               // Free-text input
  expenseDate: string;               // ISO date string (YYYY-MM-DD)
  categoryId: number | null;         // Selected category ID
  files: File[];                     // Attached files
  errors: Record<string, string>;    // Validation error messages
  loading: boolean;                  // Submission in progress
  successMessage: string | null;     // Post-submission feedback
  errorMessage: string | null;       // Submission error feedback
}
