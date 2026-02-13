package com.homebudget.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleValidationErrors")
    class HandleValidationErrors {

        @Test
        @DisplayName("should return 400 with field errors")
        void returns400WithFieldErrors() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
            bindingResult.addError(new FieldError("dto", "amount", "must not be null"));
            bindingResult.addError(new FieldError("dto", "description", "must not be blank"));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleValidationErrors(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
            assertThat(response.getBody().getErrors()).containsKey("amount");
            assertThat(response.getBody().getErrors()).containsKey("description");
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleBudgetNotFound")
    class HandleBudgetNotFound {

        @Test
        @DisplayName("should return 404 with message")
        void returns404() {
            BudgetNotFoundException ex = new BudgetNotFoundException(42L);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleBudgetNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).contains("42");
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleDuplicateBudget")
    class HandleDuplicateBudget {

        @Test
        @DisplayName("should return 409 with message")
        void returns409() {
            DuplicateBudgetException ex = new DuplicateBudgetException("Duplicate budget");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleDuplicateBudget(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(409);
            assertThat(response.getBody().getMessage()).isEqualTo("Duplicate budget");
        }
    }

    @Nested
    @DisplayName("handleParentBudgetMismatch")
    class HandleParentBudgetMismatch {

        @Test
        @DisplayName("should return 400 with detail message")
        void returns400() {
            ParentBudgetMismatchException ex = new ParentBudgetMismatchException("Mismatch detail");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleParentBudgetMismatch(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Mismatch detail");
        }
    }

    @Nested
    @DisplayName("handleIllegalArgument")
    class HandleIllegalArgument {

        @Test
        @DisplayName("should return 400 with message")
        void returns400() {
            IllegalArgumentException ex = new IllegalArgumentException("Bad input");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Bad input");
        }
    }

    @Nested
    @DisplayName("handleMaxUploadSize")
    class HandleMaxUploadSize {

        @Test
        @DisplayName("should return 413 with generic message")
        void returns413() {
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleMaxUploadSize(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(413);
            assertThat(response.getBody().getMessage()).isEqualTo("Uploaded file exceeds size limit");
        }
    }

    @Nested
    @DisplayName("handleExpenseNotFound")
    class HandleExpenseNotFound {

        @Test
        @DisplayName("should return 404 with message")
        void returns404() {
            ExpenseNotFoundException ex = new ExpenseNotFoundException(99L);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleExpenseNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).contains("99");
        }
    }

    @Nested
    @DisplayName("handleCategoryInUse")
    class HandleCategoryInUse {

        @Test
        @DisplayName("should return 409 with message")
        void returns409() {
            CategoryInUseException ex = new CategoryInUseException("Category in use");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleCategoryInUse(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(409);
            assertThat(response.getBody().getMessage()).isEqualTo("Category in use");
        }
    }

    @Nested
    @DisplayName("handleCategoryNotFound")
    class HandleCategoryNotFound {

        @Test
        @DisplayName("should return 404 with message")
        void returns404() {
            CategoryNotFoundException ex = new CategoryNotFoundException(55L);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleCategoryNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).contains("55");
        }
    }

    @Nested
    @DisplayName("handleDuplicateCategory")
    class HandleDuplicateCategory {

        @Test
        @DisplayName("should return 409 with message")
        void returns409() {
            DuplicateCategoryException ex = new DuplicateCategoryException("Groceries");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleDuplicateCategory(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(409);
            assertThat(response.getBody().getMessage()).isEqualTo("Category with name 'Groceries' already exists");
        }
    }

    @Nested
    @DisplayName("handleDatabaseException")
    class HandleDatabaseException {

        @Test
        @DisplayName("should return 500 with type info")
        void returns500() {
            DataAccessResourceFailureException ex =
                    new DataAccessResourceFailureException("Connection refused");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleDatabaseException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getMessage()).isEqualTo("Database error occurred");
            assertThat(response.getBody().getErrors()).containsKey("type");
            assertThat(response.getBody().getErrors().get("type"))
                    .isEqualTo("DataAccessResourceFailureException");
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("should return 500 with class name and masked message")
        void returns500() {
            RuntimeException ex = new RuntimeException("Something broke");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleGenericException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().getErrors()).containsKey("type");
            assertThat(response.getBody().getErrors().get("type")).isEqualTo("RuntimeException");
            assertThat(response.getBody().getErrors()).containsKey("details");
        }
    }
}
