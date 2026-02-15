class OcrError(Exception):
    def __init__(self, error_code: str, message: str, retryable: bool):
        self.error_code = error_code
        self.message = message
        self.retryable = retryable
        super().__init__(message)


class NonRetryableError(OcrError):
    """Client should not retry — the input is invalid or unprocessable."""

    def __init__(self, error_code: str, message: str):
        super().__init__(error_code=error_code, message=message, retryable=False)


class RetryableError(OcrError):
    """Temporary failure — client should retry after a delay."""

    def __init__(self, error_code: str, message: str):
        super().__init__(error_code=error_code, message=message, retryable=True)


# Error codes
UNSUPPORTED_FORMAT = "UNSUPPORTED_FORMAT"
EMPTY_CATEGORIES = "EMPTY_CATEGORIES"
FILE_TOO_LARGE = "FILE_TOO_LARGE"
NOT_A_RECEIPT = "NOT_A_RECEIPT"
NO_EXPENSE_DATA = "NO_EXPENSE_DATA"
MODEL_SERVER_UNREACHABLE = "MODEL_SERVER_UNREACHABLE"
PROCESSING_TIMEOUT = "PROCESSING_TIMEOUT"
