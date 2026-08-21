package com.veritech.BudgetKing.exception;

/**
 * Raised when a recurring transaction template cannot be scheduled or fired,
 * e.g. a template whose schedule is exhausted or whose data would never advance.
 */
public class RecurringTransactionRuntimeException extends RuntimeException {
    public RecurringTransactionRuntimeException(String message) {
        super(message);
    }
}
