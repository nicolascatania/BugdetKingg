package com.veritech.BudgetKing.exception;

/** Raised when a budget operation breaks a domain rule, e.g. duplicating a period. */
public class BudgetRuntimeException extends RuntimeException {
    public BudgetRuntimeException(String message) {
        super(message);
    }
}
