package com.veritech.BudgetKing.exception;

/**
 * Raised when a savings goal breaks a business rule (an impossible target date
 * or a non-positive target amount). Surfaces as HTTP 409 through
 * {@link GlobalExceptionHandler}.
 */
public class SavingsGoalRuntimeException extends RuntimeException {
    public SavingsGoalRuntimeException(String message) {
        super(message);
    }
}
